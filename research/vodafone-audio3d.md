# Vodafone Audio3D research and implementation

Audio3D now processes the PCM output of the existing MA-3/MA-5 renderers and
decoded WAV/MP4/3GP players. This does not add MA-7 instrument synthesis.

## Native references

The investigation used the local MEXA 2.3 binaries in Ghidra. Addresses are
virtual addresses at the original image bases: EXE `00400000`, DLL `10000000`.
No reference binaries or game assets are included in this repository.

| Binary | SHA-256 |
| --- | --- |
| mexa_emulator.exe | `41ec61a5102bbf604aa038a9a26b827e699d2f8697561c83fc94f8c939c7fc7e` |
| M7_EmuSmw7.dll | `cf5d9a8fd7d9aca95ad1960255101ec228569b3284de8dcfd9b838538e063fd4` |
| SMAFMMS7EMU.DLL | `b7d5999bad5bddf1dac01c1b1cfeaa7a5d472b0de33750567338124cdd3940ef` |

## Confirmed API behavior

These addresses are in `mexa_emulator.exe`:

| Address | Finding |
| --- | --- |
| `005117d0`, `00512930` | Four source slots and a separate available-slot counter. |
| `00511fdd`, `0051293a`, `00512963` | Enabling an extended/dynamic source reserves a slot; disabling releases it. |
| `00511ced` | Default mode 0; position/velocity zero; minimum distance 100; maximum 100000; rolloff 100; listener-relative true; reverb level 0. |
| `00511550` | Listener at origin, stationary, facing negative Z, positive Y up; reverb None. |
| `00511be7`, `00511bc0` | Active reverb decay clamps to 300–30000 milliseconds. None returns 0. Each preset remembers its own time. |
| `00511b0c`, `00511b7a` | Vodafone None index 7 disables Yamaha reverb; other indices map to Yamaha presets. |
| `00511798`, `005115f5` | Turning deferred mode off commits. Explicit commit sends positions, velocities, and listener axes for dynamic sources. |
| `00512579`, `00596f10`, `00597175` | Listener-relative toggles transform source position and velocity, preserving world placement. Axes are normalized. |
| `005120bc` | Dynamic mode uses source/listener coordinates. Extended mode centers the source but permits reverb. Absolute source velocity subtracts listener velocity; Z is negated at the Yamaha boundary. Rolloff is converted to 16.16 and halved. |
| `005129fe` | Reverb uses `Mapi_DeviceControlEx(0x10031, ...)`: enable command 1, preset 12, read parameters 8, write parameters 7; decay mask `0x40`. |
| `00512e00` | Source parameters use `Mapi_Melody_Control(handle, 0xa2, state)` then commit command `0xa4`. |

Native default decay times, in milliseconds:

| Arena | Bathroom | Cave | City | Concert Hall | Forest | Mountains | None | Room | Under Water |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1800 | 1480 | 2910 | 1490 | 1800 | 1490 | 1490 | 0 | 400 | 1490 |

The third `setRolloff` argument is an attenuation factor, not a mute-distance
flag. Yamaha has a separate flag for that, which MEXA leaves zero.

## Yamaha implementation trace

In `M7_EmuSmw7.dll`, `10002280` binds implementations by operating mode.
The default device handler `1000d850` translates reverb operations into internal
commands `0x42`, `0x54`, `0x55`, `0x58`, `0x59`, and `0x5a`, forwarded through
`10023400` / `1001fee0` to `10055170`.

`100519f0` independently enforces the 300–30000 ms range. `1006a4b0` selects a
preset using data at `1035a018` and decay data at `10357db0`; `100d1f70` applies
custom decay. The SMAFMMS wrapper `10010c20` queues device commands to worker
`10010d50`, dispatched at `10010f23`, then forwards through its sound-driver
table. It is not the synthesis or room-processing implementation.

Source parameters are validated/mapped by `100514c0`, committed by `10138590`,
and processed by `10069480`. Relevant downstream functions:

- `10069030` clamps minimum distance to at least 100 and measured distance to
  the configured maximum. It converts `minimum / distance` to centibels, then
  multiplies by the rolloff coefficient. `1006ad70` interpolates the logarithm
  table at `1078dd58`; ratio 0.5 gives -602 centibels. `1006a850` converts back
  to linear amplitude. With MEXA's halved coefficient, the floating-point
  equivalent is `(minimum / clampedDistance) ** (factor / 200.0)`.
- `10211db0` computes velocity-dependent pitch using fixed-point math. Its
  exact coefficient scaling has not been reconstructed.
- `10053de0` writes separate 23-coefficient ear filters and gains to hardware
  registers. The original therefore provides more than stereo panning. Those
  filter tables and the hardware reverb network have not been ported.

## Software processing

`Audio3DScene` owns the listener/environment per appli. Pending and committed
placements are separate. One short lock snapshots listener and source together;
neither rendering nor device writes hold it. Rolloff, mode, reverb, and output
device remain immediate when placement is deferred. Weak source references and
weak class-loader keys avoid retaining old applications.

`SpatialAudioStream` runs after synthesis/decoding and before final volume/mute:

- Stereo point-source placement, native distance attenuation, listener rotation,
  relative/absolute coordinates, and relative velocity.
- Smoothed control changes, headphone interaural delay/head shadow, and an
  analytic pinna notch for elevation and front/back coloration. Headphones
  preserve a quieter far-ear signal; speakers use stereo panning.
- Doppler resampling with coordinates interpreted as millimetres, velocity as
  millimetres/second, and sound speed 343000 mm/s. The physical approximation
  is bounded to 0.5–2 times normal speed. A 24-tap windowed-sinc filter suppresses
  aliasing when pitching up. Velocity changes pitch; applications update position.
- An eight-delay stereo feedback network with damping, predelay, and distinct
  software room profiles. Native decay times and API send levels control it.
  Storage stays bounded even at a 30-second decay.

The room profiles, ear filters, and Doppler formula are approximations, not
waveform-accurate Yamaha emulation. Sample rate is preserved. Disabled mode
preserves stereo samples and duplicates mono into stereo. Dynamic mode downmixes
the player into one point source. Extended mode retains stereo and adds reverb.

Loop boundaries precede processing, so loops retain their room state without
inserting tails. Final playback drains a bounded tail before completion:
at most `1.25 * decay + 0.4` seconds. Pause freezes processing; resume retains
state; restart resets it. Stop/close remove output. Mute scales dry and wet
together. Disabling the mode, choosing None, or setting send to zero clears
old reverb state.

Host MIDI-device fallback does not expose PCM and therefore does not receive
these effects. WAVs with more than two channels retain their previous playback
without Audio3D processing. Existing MMAPI clocks have not been redesigned
around Doppler-adjusted device timestamps. MA-7 synthesis remains unimplemented.

## Validation and the reported application

Junji Inagawa's Ghost Stories Trial selects headphones and **None** reverb.
Its positional effects now work; ReMEXA does not add room reverb unless requested.
The app sets send level 70 and alternates effects at X = -1000 and +1000.

- 97 tests pass with `mvn -Dremexa.test.vlc=true test`, including actual MA-3
  and MA-5 output through the effects stage and the VLC decoder.
- Signal tests cover direction, distance, Doppler pitch/duration, alias rejection,
  elevation/headphone cues, decay, tails, reset, and chunk boundaries.
- State/lifecycle tests cover deferred commits, coordinate conversion, appli
  isolation, loop continuity, mute, pause, and close. PCM and SMAF mixers are
  compared sample-for-sample with identical looping sources and reverb.
- Local probes resolve all 59 external API references and render directional
  output from all 13 actual sound assets. All reserved slots are released.
- The original MIDlet completes the first narration, plays its alternating
  left/right dynamic effects, and advances into story 2.

The application fixtures stay outside the repository. `mvn test` works without
them; the optional VLC decoder check requires VLC 3.x on the host.
