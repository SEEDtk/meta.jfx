/**
 *
 */
package org.theseed.dl4j.jfx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.dl4j.train.RunLog;
import org.theseed.io.LineReader;
import org.theseed.jfx.ResizableController;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * This is a modeless dialog that allows the user to view the current model's trial log.  The log is
 * organized into a hierarchy, with one or more jobs, each containing multiple sections and a summary.
 *
 * @author Bruce Parrello
 *
 */
public class LogViewer extends ResizableController {

    // FIELDS
    /** last summary node loaded into the tree */
    private TreeItem<LogSection> lastSummary;
    /** root icon */
    private static final Image ROOT_ICON = new Image(App.class.getResourceAsStream("model-16.png"));
    /** job icon */
    private static final Image JOB_ICON = new Image(App.class.getResourceAsStream("job-16.png"));
    /** session icon */
    private static final Image SESSION_ICON = new Image(App.class.getResourceAsStream("session-16.png"));
    /** summary icon */
    private static final Image SUMMARY_ICON = new Image(App.class.getResourceAsStream("summary-16.png"));
    /** cross-validate icon */
    private static final Image CROSS_ICON = new Image(App.class.getResourceAsStream("validate-16.png"));
    /** line separator for creating section text */
    private static final String LINE_SEP = System.getProperty("line.separator");
    /** default number of lines to allocate for each section */
    private static final int SECTION_SIZE = 80;

    // CONTROLS

    /** tree display */
    @FXML
    private TreeView<LogSection> treeDirectory;

    /** log section display */
    @FXML
    private TextArea sectionDisplay;

    /**
     * This object represents the data for a single section.  It contains the section name and its
     * data lines, or null if there is no data.
     */
    private static class LogSection {

        private String name;
        private List<String> lines;

        /**
         * Create an empty section.
         *
         * @param name		name of section
         */
        public LogSection(String name) {
            this.name = name;
            this.lines = new ArrayList<String>(SECTION_SIZE);
        }

        /**
         * Add a line to this section.
         *
         * @param line		line to add
         */
        public void add(String line) {
            this.lines.add(line);
        }

        @Override
        public String toString() {
            return this.name;
        }

        /**
         * @return TRUE if this section has no lines, FALSE if it has data in it
         */
        public boolean isEmpty() {
            return this.lines.isEmpty();
        }

        /**
         * @return the data string for this section
         */
        public String getLines() {
            return StringUtils.join(this.lines, LINE_SEP);
        }

    }


    /**
     * Create the log viewer window.
     */
    public LogViewer() {
        super(200, 200, 1000, 1000);
    }

    @Override
    public String getIconName() {
        return "log-page-16.png";
    }

    @Override
    public String getWindowTitle() {
        return "Trial Log Viewer";
    }

    /**
     * Initialize this window.
     *
     * @param logFile		file containing the log to load
     * @param modelName		model name
     *
     * @throws IOException
     */
    public void init(File logFile, String modelName) throws IOException {
        boolean found = false;
        this.lastSummary = null;
        // Create the root node.
        TreeItem<LogSection> rootNode = new TreeItem<LogSection>(new LogSection(modelName), new ImageView(ROOT_ICON));
        this.treeDirectory.setRoot(rootNode);
        // Everything else comes from the file.
        try (LineReader reader = new LineReader(logFile)) {
            Iterator<String> lineIter = reader.iterator();
            // Find the first job.
            while (lineIter.hasNext() && ! lineIter.next().contentEquals(RunLog.JOB_START_MARKER));
            // We are now positioned at the end of file or immediately after a job-start marker.
            while (lineIter.hasNext()) {
                // Consume this job.
                boolean ok = this.readJob(rootNode, lineIter);
                // If it had sections, denote the tree is valid.
                if (ok) found = true;
            }
            // If we found nothing, it's an error.
            if (! found)
                throw new IOException("Trial log had no valid jobs in it.");
            // If we found a summary, pre-select it.
            if (lastSummary != null) {
                this.treeDirectory.getSelectionModel().select(lastSummary);
                this.showSection(lastSummary);
            }
        }
        // Create a listener to update the text area when an item is selected
        this.treeDirectory.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<LogSection>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<LogSection>> observable,
                    TreeItem<LogSection> oldValue, TreeItem<LogSection> newValue) {
                showSection(newValue);
            }
        });

    }

    /**
     * Display the specified section in the text area.
     *
     * @param sectionItem	item selected
     */
    private void showSection(TreeItem<LogSection> sectionItem) {
        LogSection section = sectionItem.getValue();
        // If there is data in the section, display it in the text pane.
        if (! section.isEmpty())
            this.sectionDisplay.setText(section.getLines());
    }

    /**
     * Read the job at the current position.
     *
     * @param rootNode		root node used as parent for the job
     * @param lineIter		iterator for the input file
     *
     * @return TRUE if a nonempty job was found, else FALSE
     */
    private boolean readJob(TreeItem<LogSection> rootNode, Iterator<String> lineIter) {
        boolean retVal = false;
        String line = lineIter.next();
        LogSection jobSection = new LogSection(line);
        Image icon = (line.startsWith("Cross-Validate") ? CROSS_ICON : JOB_ICON);
        TreeItem<LogSection> jobItem = new TreeItem<LogSection>(jobSection, new ImageView(icon));
        rootNode.getChildren().add(jobItem);
        // This will be the current section.
        LogSection section = null;
        // Loop until we find the end of the file or an end-of-job marker.
        boolean endOfJob = false;
        while (lineIter.hasNext() && ! endOfJob) {
            // Get the next line.
            line = lineIter.next();
            switch (line) {
            case RunLog.JOB_START_MARKER :
                // Here we are at the end of the job.
                endOfJob = true;
                break;
            case RunLog.TRIAL_SECTION_MARKER :
                // Here we are at the start of a new section.
                if (section != null) {
                    this.storeSection(jobItem, section);
                    // Denote we do not have a section in progress.
                    section = null;
                    // We have a section.  Return TRUE.
                    retVal = true;
                }
                // If we do NOT have premature end-of-file, start the new section.
                // The section title is on the next line.
                if (lineIter.hasNext()) {
                    line = lineIter.next();
                    section = new LogSection(line);
                    section.add(line);
                }
                break;
            default :
                // If we are in a section, this is a data line.  Otherwise, it is a spacer and we ignore it.
                if (section != null)
                    section.add(line);
            }
        }
        // Save the residual.
        if (section != null) {
            this.storeSection(jobItem, section);
            retVal = true;
        }
        return retVal;
    }

    /**
     * Store the current section in the tree.
     *
     * @param jobItem	parent job
     * @param section	section to store
     */
    private void storeSection(TreeItem<LogSection> jobItem, LogSection section) {
        Image icon = (section.toString().startsWith("Summary") ? SUMMARY_ICON : SESSION_ICON);
        TreeItem<LogSection> sectionItem = new TreeItem<LogSection>(section, new ImageView(icon));
        jobItem.getChildren().add(sectionItem);
        // If this was a summary section, remember it.
        if (icon == SUMMARY_ICON)
            this.lastSummary = sectionItem;
    }

}
