import SwiftUI
import BackgroundTasks

@main
struct iOSApp: App {
    init() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.dokuha.reader.library.refresh",
            using: nil
        ) { task in
            task.setTaskCompleted(success: true)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}
