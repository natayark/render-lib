phire::tl_file!("offset");

use std::collections::VecDeque;
use super::{Page, SharedState};
use crate::{get_data, get_data_mut, save_data};
use anyhow::{Context, Result};
use macroquad::prelude::*;
use phire::{
    core::{ParticleEmitter, ResourcePack, NOTE_WIDTH_RATIO_BASE},
    ext::{create_audio_manger, get_latency, push_frame_time, screen_aspect, semi_black, RectExt, SafeTexture, ScaleType},
    time::TimeManager,
    ui::{Slider, Ui},
};
use sasa::{AudioClip, AudioManager, Music, MusicParams, PlaySfxParams, Sfx};

pub struct OffsetPage {
    audio: AudioManager,
    cali: Music,
    cali_hit: Sfx,

    tm: TimeManager,
    cali_last: bool,

    click: SafeTexture,
    _hit_fx: SafeTexture,
    emitter: ParticleEmitter,
    color: Color,

    slider: Slider,

    touched: bool,
    touch: Option<(f32, f32, f32)>,

    frame_times: VecDeque<f64>, // frame interval time
    latency_record: VecDeque<f32>,
}

impl OffsetPage {
    const FADE_TIME: f32 = 0.8;

    pub async fn new() -> Result<Self> {
        let mut audio = create_audio_manger(&get_data().config)?;
        let cali = audio.create_music(
            AudioClip::new(load_file("cali.ogg").await?)?,
            MusicParams {
                loop_mix_time: 0.,
                ..Default::default()
            },
        )?;
        let cali_hit = audio.create_sfx(AudioClip::new(load_file("cali_hit.ogg").await?)?, None)?;

        let mut tm = TimeManager::new(1., true);
        tm.force = 3e-2;

        let respack = ResourcePack::from_path(get_data().config.res_pack_path.as_ref())
            .await
            .context("Failed to load resource pack")?;
        let click = respack.note_style.click.clone();
        let emitter = ParticleEmitter::new(&respack, get_data().config.note_scale, respack.info.hide_particles, None)?;

        let frame_times: VecDeque<f64> = VecDeque::new();
        let latency_record: VecDeque<f32> = VecDeque::new();
        Ok(Self {
            audio,
            cali,
            cali_hit,

            tm,
            cali_last: false,

            click,
            _hit_fx: respack.hit_fx,
            emitter,
            color: respack.info.fx_perfect(),

            slider: Slider::new(-500.0..500.0, 5.),

            touched: false,
            touch: None,

            frame_times,
            latency_record,
        })
    }
}

impl Page for OffsetPage {
    fn can_play_bgm(&self) -> bool {
        false
    }

    fn label(&self) -> std::borrow::Cow<'static, str> {
        "CALIBRATION".into()
    }

    fn exit(&mut self) -> Result<()> {
        save_data()?;
        Ok(())
    }

    fn enter(&mut self, _s: &mut SharedState) -> Result<()> {
        self.cali.seek_to(0.)?;
        self.cali.play()?;
        self.tm.reset();
        Ok(())
    }

    fn pause(&mut self) -> Result<()> {
        save_data()?;
        self.tm.pause();
        self.cali.pause()?;
        Ok(())
    }

    fn resume(&mut self) -> Result<()> {
        self.tm.resume();
        self.cali.play()?;
        Ok(())
    }

    fn touch(&mut self, touch: &Touch, s: &mut SharedState) -> Result<bool> {
        let t = s.t;
        let config = &mut get_data_mut().config;
        let mut offset = config.offset * 1000.;
        if self.slider.touch(touch, t, &mut offset).is_some() {
            config.offset = offset / 1000.;
            return Ok(true);
        }
        let x = touch.position.x;
        let y = touch.position.y * screen_aspect();
        if touch.phase == TouchPhase::Started
            && (-0.80..0.00).contains(&x)
            && (-0.73..0.94).contains(&y)
        {
            self.touched = true;
        }
        Ok(false)
    }

    fn update(&mut self, _s: &mut SharedState) -> Result<()> {
        if !self.cali.paused() {
            let pos = self.cali.position() as f64;
            let now = self.tm.now();
            if now > 2. {
                self.tm.seek_to(now - 2.);
                self.tm.dont_wait();
            }
            let now = self.tm.now();
            if now - pos >= -1. {
                self.tm.update(pos);
            }
        }
        Ok(())
    }

    fn render(&mut self, ui: &mut Ui, s: &mut SharedState) -> Result<()> {
        let t = s.t;
        s.render_fader(ui, |ui, c| {
            let lf = -0.92;
            let mut r = ui.content_rect();
            r.w += r.x - lf;
            r.x = lf;
            ui.fill_path(&r.rounded(0.02), semi_black(c.a * 0.4));

            let ct = (-0.4, r.bottom() - 0.12);
            let hw = 0.4;
            let hh = 0.0075;
            ui.fill_rect(Rect::new(ct.0 - hw, ct.1 - hh / 2., hw * 2., hh), c);

            let ot = t;

            let config = &get_data().config;
            let mut t = self.tm.now() as f32 - config.offset;

            if config.adjust_time {
                let latency = get_latency(&self.audio, &self.frame_times);
                t -= latency;
                ui.text(format!("{} {:.0}ms", tl!("estimated"), latency * 1000.))
                    .pos(0.54, 0.24)
                    .anchor(0.5, 1.)
                    .size(0.5)
                    .color(Color::new(1., 1., 1., 0.8))
                    .draw();
            }

            if t < 0. {
                t += 2.;
            }
            if t >= 2. {
                t -= 2.;
            }
            let ny = ct.1 + (t - 1.) * 0.75;
            if self.touched {
                self.touch = Some((t, ot, ny));
                self.touched = false;
                self.cali_hit.play(PlaySfxParams {
                    amplifier: config.volume_sfx,
                }).unwrap();
            }
            if t <= 1. {
                let w = NOTE_WIDTH_RATIO_BASE * config.note_scale * 2.;
                let h = w * self.click.height() / self.click.width();
                let r = Rect::new(ct.0 - w / 2., ny - h / 2., w, h);
                ui.fill_rect(r, (*self.click, r, ScaleType::Fit, c));
                self.cali_last = true;
            } else {
                if self.cali_last {
                    let g = ui.to_global(ct);
                    self.emitter.emit_at(vec2(g.0, g.1), 0., self.color);
                }
                self.cali_last = false;
            }

            if let Some((diff, time, pos)) = &self.touch {
                let p = (ot - time) / Self::FADE_TIME;
                if p > 1. {
                    self.touch = None;
                } else {
                    let p = p.max(0.);
                    let c = Color {
                        a: (if p <= 0.5 { 1. } else { (1. - p) * 2. }) * c.a * self.color.a,
                        ..self.color
                    };
                    ui.fill_rect(Rect::new(ct.0 - hw, pos - hh / 2., hw * 2., hh), c);
                    let latency = diff - 1.;
                    if latency.abs() < 0.200 {
                        self.latency_record.push_back(latency);
                        if self.latency_record.len() > 10 {
                            self.latency_record.pop_front();
                        }
                    }
                    ui.text(format!("{} {:.0}ms", tl!("now"), latency * 1000.))
                        .pos(0.54, 0.10)
                        .anchor(0.5, 1.)
                        .size(0.5)
                        .color(Color::new(1., 1., 1., 0.8 * c.a))
                        .draw();
                }
            }

            let avg_latency = if self.latency_record.is_empty() {
                0.0
            } else {
                self.latency_record.iter().sum::<f32>() / self.latency_record.len() as f32
            };
            ui.text(format!("{} {:.0}ms", tl!("avg"), avg_latency * 1000.))
                .pos(0.54, 0.17)
                .anchor(0.5, 1.)
                .size(0.5)
                .color(Color::new(1., 1., 1., 0.8))
                .draw();

            let offset = config.offset * 1000.;
            self.slider
                .render(ui, Rect::new(0.46, -0.1, 0.45, 0.2), ot, c, offset, format!("{offset:.0}ms"));

            if config.adjust_time {
                push_frame_time(&mut self.frame_times, self.tm.real_time());
            }
        });

        self.emitter.draw(get_frame_time());

        Ok(())
    }
}
