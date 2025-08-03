Gemini Assistant Glyph Animation - A Nothing SDK Project
This project is a modification of the official GlyphMatrix-Example-Project from Nothing. It adds a new, standalone feature: a custom Glyph animation that automatically plays whenever the Google Assistant (Gemini) is actively listening.

The Feature
The core goal was to create a visual cue on the back of the phone to confirm that the assistant is listening, especially when the screen is off or the phone is face down.

Automatic Trigger: A pulsing microphone animation starts the moment the assistant is activated.

Hands-Free Confirmation: Works on the lock screen, providing a clear visual that your "Hey Google" was heard.

Automatic Stop: The animation plays for a set duration (currently 3 seconds) and then stops, releasing the Glyph hardware.

How It Works: The Architecture
Instead of using the "Glyph Toy" framework, this feature uses a custom background service architecture:

GeminiListenerService.kt (AccessibilityService): This is the "detector." It's a highly efficient service that does nothing but watch for the Google Assistant's UI to appear on screen.

GlyphAnimationService.kt (ForegroundService): This is the "animator." When the detector sees the assistant, it commands this service to start. The service then takes control of the Glyph hardware, plays the pulsing microphone animation, and cleans up after itself.

Key Files to Look At
All the new logic is self-contained in a few key files. If you want to see how it works, check these out:

app/src/main/java/com/nothinglondon/sdkdemo/GeminiListenerService.kt: The accessibility service that detects the assistant.

app/src/main/java/com/nothinglondon/sdkdemo/GlyphAnimationService.kt: The foreground service that controls the hardware and contains the animation logic.

app/src/main/AndroidManifest.xml: This file was modified to register the two new services and add the necessary permissions for them to run in the background.

How to Use
Build and install the app on your Nothing phone.

Open the app once. You'll see a single button: "Enable Gemini Glyph Service."

Tap the button. This will take you to your phone's Accessibility settings.

Find "Gemini Glyph Listener" in the list of downloaded apps and turn it on.

That's it! The service will now run in the background. The next time you activate the assistant, the Glyph animation will play.

Known Issues
Hardware Release Bug: There appears to be a bug in the current version of the Glyph Developer Kit where calling unInit() does not fully release the hardware lock. After our animation plays, other apps (like the voice recorder) cannot use the Glyphs until the screen is locked and unlocked. We have reported this issue on the Nothing developer forums.
