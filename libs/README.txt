This directory holds compile-only mod JARs that are NOT redistributed via this
repository (their licenses don't permit republication, and they don't need to
be bundled into our final JAR — users install them separately).

Before building, please download the following JAR from CurseForge or Modrinth
and place it here:

  - BetterAdvancements-NeoForge-1.21.1-0.4.3.21.jar
    Source: https://www.curseforge.com/minecraft/mc-mods/better-advancements

build.gradle references this jar via `compileOnly files("libs/...")`, so it is
only needed at compile time. Users who install our mod will supply their own
BetterAdvancements installation at runtime (or skip it — our mod silently
detects whether BA is present).
