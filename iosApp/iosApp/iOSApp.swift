import SwiftUI
import BackgroundTasks
import ComposeApp

@main
struct iOSApp: App {
    init() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.dokuha.reader.library.refresh",
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }

            LibraryUpdateScheduler.shared.schedulePeriodicUpdates(intervalHours: 12)
            let cancelRefresh = LibraryUpdateScheduler.shared.performBackgroundRefresh(
                onSuccess: {
                    refreshTask.setTaskCompleted(success: true)
                },
                onFailure: {
                    refreshTask.setTaskCompleted(success: false)
                }
            )
            refreshTask.expirationHandler = {
                cancelRefresh()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}
