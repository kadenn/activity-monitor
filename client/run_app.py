import screen_watcher
import window_watcher
import multiprocessing

if __name__ == '__main__':
    multiprocessing.freeze_support()
    screen_watcher_process = multiprocessing.Process(target=screen_watcher.screen_watcher_loop, args=(30,))
    screen_watcher_process.start()
    window_watcher_process = multiprocessing.Process(target=window_watcher.window_watcher_loop, args=(30,))
    window_watcher_process.start()

