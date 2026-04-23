# ReMEXA

ReMEXA is a modern Java reimplementation for legacy SoftBank MEXA S-Appli, Vodafone V-Appli, and J-Sky V-Appli runtimes.

## Common Commands

```powershell
mvn -q -DskipTests package
.\scripts\run.ps1
.\scripts\run.ps1 --run-jad "D:\example.jad"
```

## Layout

- `src/main/java/remexa/...`: host runtime, UI, logging, and project-owned code
- `src/main/java/javax/...`: small MIDP compatibility layer for groundwork
- `src/tools/java/remexa/tools/sdkstub/...`: Javadoc-to-stub generator
- `src/java/...`:  legacy SDK apis