package us.deathmarine.luyten;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import java.io.File;

/**
 * An OS X-specific initialization method for dragging/dropping
 */
public class LuytenOsx extends Luyten {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.d3d", "false");
        // Add an adapter as the handler to a new instance of the application
        // class
        @SuppressWarnings("deprecation")
        Application app = new Application();
        app.addApplicationListener(new ApplicationAdapter() {
            public void handleOpenFile(ApplicationEvent e) {
                Luyten.addToPendingFiles(new File(e.getFilename()));
                Luyten.processPendingFiles();
            }

            public void handleQuit(ApplicationEvent e) {
                Luyten.quitInstance();
            }
        });

        // Call the superclass's main function
        Luyten.main(args);
    }
}
