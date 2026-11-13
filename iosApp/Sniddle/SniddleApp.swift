// The whole iOS app on the Swift side: one full-screen window that hosts the
// shared Compose Multiplatform UI. Everything else (the game, its drawing,
// sound and saving) is Kotlin. See docs/ios.md.
//
// Until the shared UI module exists (phase 2), the Kotlin framework isn't
// there to import, and this builds a placeholder screen instead, so the build
// and install path can be tried on a real iPhone or iPad before the port.

import SwiftUI
import UIKit

#if canImport(ComposeApp)
    // PHASE 2 ASSUMPTION: framework baseName "ComposeApp", exposing
    // fun MainViewController(): UIViewController from MainViewController.kt.
    import ComposeApp
#endif

@main
struct SniddleApp: App {
    init() {
        // Keep the screen awake while Sniddle is open, like Android's
        // FLAG_KEEP_SCREEN_ON. iOS only applies it while the app is in front.
        UIApplication.shared.isIdleTimerDisabled = true
    }

    var body: some Scene {
        WindowGroup {
            GameScreen()
                // The arena draws edge to edge; the Compose UI keeps its
                // buttons and score clear of the notch and the home indicator
                // with WindowInsets.safeDrawing.
                .ignoresSafeArea()
                // Full screen, like Android's immersive mode: no status bar,
                // and the home indicator fades out. A swipe up from the bottom
                // edge (or down from the top) first only shows the indicator
                // (or Control Centre's grabber); a second swipe acts. A kid's
                // thumb sliding off the edge mid-game doesn't leave the game.
                .statusBarHidden(true)
                .persistentSystemOverlays(.hidden)
                .defersSystemGestures(on: .all)
        }
    }
}

private struct GameScreen: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        #if canImport(ComposeApp)
            return MainViewControllerKt.MainViewController()
        #else
            return UIHostingController(rootView: Placeholder())
        #endif
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

#if !canImport(ComposeApp)
    /// Shown only by a build without the Kotlin UI: proves the app installs and launches.
    private struct Placeholder: View {
        var body: some View {
            ZStack {
                Color("LaunchBackground")
                VStack(spacing: 12) {
                    Text("Sniddle")
                        .font(.system(size: 48, weight: .heavy, design: .rounded))
                    Text("The snails are on their way to iPhone and iPad.")
                        .font(.system(.title3, design: .rounded))
                    Text(versionLine)
                        .font(.footnote.monospacedDigit())
                        .opacity(0.6)
                }
                .multilineTextAlignment(.center)
                .foregroundStyle(Color(red: 0.07, green: 0.07, blue: 0.15))
                .padding()
            }
        }

        private var versionLine: String {
            let info = Bundle.main.infoDictionary ?? [:]
            let name = info["CFBundleShortVersionString"] as? String ?? "?"
            let build = info["CFBundleVersion"] as? String ?? "?"
            return "\(name) (\(build))"
        }
    }
#endif
