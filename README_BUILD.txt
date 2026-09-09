THE FORGE  |  Copyright (c) 2026 Yannie D. Forest. All rights reserved.
NO LICENCE IS GRANTED. No copying, adaptation, redistribution, reverse
engineering or commercial use without the author's written permission.
See LICENCE.txt. Every distributed copy carries a build marker.
--------------------------------------------------------------------------

BUILDING THE THE FORGE APK FOR YOUR FIRE TABLET
=================================================

This project turns The Forge into a real Android app: a single installable APK
that shows the seal on the home screen and runs completely offline. Fire OS is
Android underneath, so this APK sideloads onto a Fire tablet directly, with no app
store and no browser.

You do NOT need to write any code. Choose ONE of the three routes below. The first
needs nothing installed on your computer at all.

--------------------------------------------------------------------------------
ROUTE 1 (EASIEST, NOTHING TO INSTALL): LET GITHUB BUILD THE APK FOR YOU
--------------------------------------------------------------------------------
This uses GitHub's free build machines. You upload the project once and download a
finished APK a few minutes later.

1. Make a free account at github.com if you do not have one.
2. Click New repository. Name it anything (for example the-forge-app). You may
   keep it Private. Create it.
3. On the new repository page, click "uploading an existing file." Drag in the
   CONTENTS of this android folder (everything you see here: the app folder, the
   .github folder, build.gradle, settings.gradle, and so on). Commit.
4. GitHub starts building automatically. Click the "Actions" tab and wait for the
   green check (about three to five minutes).
5. For a tablet, use the repository's Releases page and tap the-forge.apk.
   An Actions artifact needs a signed-in browser and downloads as a zip, so a
   tablet cannot fetch it. On a computer where you are signed in, the
   "Artifacts" link on the run still works and gives the same APK.
   Unzip it to get app-debug.apk. That is your app.

Then jump to INSTALLING ON THE TABLET below.

--------------------------------------------------------------------------------
ROUTE 2 (ONE FREE TOOL): ANDROID STUDIO, ONE CLICK
--------------------------------------------------------------------------------
1. Install Android Studio (free, from developer.android.com/studio).
2. Open it, choose Open, and select this android folder.
3. Let it finish "Gradle sync" the first time (it downloads what it needs).
4. Menu: Build, then Build Bundle(s) / APK(s), then Build APK(s).
5. When it finishes, click "locate" to find app-debug.apk (under
   app/build/outputs/apk/debug/). That is your app.

--------------------------------------------------------------------------------
ROUTE 3 (COMMAND LINE): IF YOU ALREADY HAVE THE ANDROID SDK
--------------------------------------------------------------------------------
From inside this android folder:
    gradle wrapper --gradle-version 8.7
    ./gradlew :app:assembleDebug
The APK lands at app/build/outputs/apk/debug/app-debug.apk.

--------------------------------------------------------------------------------
INSTALLING THE APK ON THE FIRE TABLET (all routes end here)
--------------------------------------------------------------------------------
1. On the tablet: Settings, then Security & Privacy, then turn on "Apps from
   Unknown Sources" (allow it for the Files app, or for whatever you will use to
   open the APK).
2. Put app-debug.apk on the tablet:
      - by USB: connect the tablet to your computer with the charging cable; it
        appears as a drive; copy the APK into the Download folder; OR
      - by email: email the APK to yourself and open it on the tablet.
3. On the tablet, open the Files app, find app-debug.apk, and tap it. Approve the
   install.
4. The Forge now appears in your app list and on the home screen, with the seal
   as its icon. It opens full screen, runs offline, and saves your work on the
   tablet. No browser, no account, nothing sent anywhere.

WHY THERE IS NO READY-MADE APK IN THIS DOWNLOAD
Compiling an Android APK requires Google's Android build tools, which cannot run in
the workspace that produced these files, and which cannot be reached from it. The
three routes above each run those tools for you, Route 1 without installing a single
thing on your own machine.

FAILSAFES IN THE WORKFLOW (added 16 August 2026)
----------------------------------------------
1. Gradle is downloaded at a pinned version instead of relying on whatever the
   runner image ships. A runner carrying Gradle 9 cannot configure Android
   Gradle Plugin 8, and the run dies before compiling. This removes that class
   of failure entirely, and it also means no gradle-wrapper.jar is needed.
2. A second job runs automatically if the first fails, stepping the project back
   to Android Gradle Plugin 7.4.2, Gradle 7.6.4 and compileSdk 33. Those edits
   are made inside that run only; the repository keeps the modern settings. Its
   artefact is named the-forge-apk-fallback.
3. Build logs are uploaded whether the run passes or fails, as build-log-current
   and build-log-fallback, so a red run always leaves something to read.
There is still no "|| true" anywhere and if-no-files-found is error, so a failed
build cannot finish green with no APK.

--------------------------------------------------------------------------
IF A CLOUD BUILD EVER SAYS "does not contain a Gradle build"
--------------------------------------------------------------------------
  That message means Gradle was run in a folder with no settings.gradle in
  it, which happens when the project is uploaded as a subfolder while the
  recipe expects it at the top.

  This is now handled. The recipe's first step searches the repository for
  settings.gradle and builds in whichever folder holds it, so BOTH of these
  work:

      your-repo/settings.gradle                      (contents uploaded)
      your-repo/the_forge_android_project/settings.gradle   (folder uploaded)

  If you are using the pasted recipe from GITHUB_WORKFLOW_paste_this.txt,
  make sure it is the current one: it should contain a step named
  "Find the Gradle project". An older pasted copy also called ./gradlew,
  which cannot work here because this project ships no wrapper script.


--------------------------------------------------------------------------
IF THE CLOUD BUILD STILL PRODUCES NO APK
--------------------------------------------------------------------------
  FIRST, AND MOST IMPORTANT: you do not need an APK to use The Forge on the
  tablet. Copy the_forge.html to the tablet and open it in Silk, then use
  the browser's "Add to Home Screen". It runs offline, it keeps your work,
  and it needs no build at all. The APK only removes the browser's own bars.

  IF YOU DO WANT THE APK, the run now tells you why it failed on its own
  page. Open the failed run and read the panel under the tick: the last 60
  lines of the build log are printed there. Send me that text and the cause
  can be named exactly rather than guessed.

  THREE THINGS THAT HAVE ACTUALLY GONE WRONG BEFORE, all now handled:
    1. The recipe was run at the top of the repository while the project sat
       in a subfolder. It now finds settings.gradle wherever it is.
    2. An older pasted recipe called ./gradlew. This project ships no wrapper
       script, so that could never work. It now calls gradle directly at a
       pinned version.
    3. The runner gave Gradle a small heap and the six megabyte page
       exhausted it, which reads as "Java heap space" and looks like a fault
       in the project. gradle.properties now asks for a larger heap.

  MAKE SURE THE RECIPE IN YOUR REPOSITORY IS THE CURRENT ONE. Open
  .github/workflows/build-apk.yml on GitHub and look for a step named
  "Find the Gradle project". If it is not there, replace the whole file with
  GITHUB_WORKFLOW_paste_this.txt from this folder and commit.

  BUILDING WITHOUT THE CLOUD, if you ever prefer it: install Android Studio,
  choose Open, point it at the_forge_android_project, and let it sync. Then
  Build > Build Bundle(s) / APK(s) > Build APK(s). The APK appears under
  app/build/outputs/apk/debug/.
