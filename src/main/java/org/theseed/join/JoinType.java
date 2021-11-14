/**
 *
 */
package org.theseed.join;

import java.io.IOException;
import java.util.Arrays;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;


/**
 * This enumeration represents a type of join operation.  We can either use the join as
 * an inclusion filter, as an exclusion filter, as a normal join, a left join, or an
 * excel store.
 *
 * @author Bruce Parrello
 *
 */
public enum JoinType {
    INIT {
        @Override
        protected IJoinSpec createController() {
            return new FileSpec();
        }

        @Override
        protected String getFxml() {
            return "JoinSpec.fxml";
        }

        @Override
        public String toString() {
            return "Input File";
        }

    }, ANALYZE {
        @Override
        protected IJoinSpec createController() {
            return new AnalyzeSpec();
        }

        @Override
        protected String getFxml() {
            return "AnalyzeSpec.fxml";
        }

        @Override
        public String toString() {
            return "Analyze Column Bias";
        }

    }, FILTER {
        @Override
        protected IJoinSpec createController() {
            return new FilterSpec();
        }

        @Override
        protected String getFxml() {
            return "FilterSpec.fxml";
        }

        @Override
        public String toString() {
            return "File Filtering";
        }

    }, JOIN {
        @Override
        protected IJoinSpec createController() {
            return new JoinSpec();
        }

        @Override
        protected String getFxml() {
            return "JoinSpec.fxml";
        }

        @Override
        public String toString() {
            return "File Join";
        }

    }, EXCELSAVE {
        @Override
        protected IJoinSpec createController() {
            return new ExcelSaveSpec();
        }

        @Override
        protected String getFxml() {
            return "ExcelSaveSpec.fxml";
        }

        @Override
        public String toString() {
            return "Save to Excel";
        }

    }, FILESAVE {
        @Override
        protected IJoinSpec createController() {
            return new FileSaveSpec();
        }

        @Override
        protected String getFxml() {
            return "FileSaveSpec.fxml";
        }

        @Override
        public String toString() {
            return "Save to Flat File";
        }

    };

    /**
     * Create the controller for a join spec of this type.
     *
     * @param parent	parent join dialog
     *
     * @return the controller of the join spec
     *
     * @throws IOException
     */
    public IJoinSpec getController(JoinDialog parent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(JoinType.class.getResource(this.getFxml()));
        fxmlLoader.setController(this.createController());
        Node displayNode = fxmlLoader.load();
        // Initialize the join specification.
        IJoinSpec retVal = (IJoinSpec) fxmlLoader.getController();
        retVal.init(parent, displayNode);
        retVal.setTitle(this.toString());
        return retVal;

    }

    /**
     * @return a new controller object for this type of join spec
     */
    protected abstract IJoinSpec createController();

    /**
     * @return the resource name for this join type
     */
    protected abstract String getFxml();

    /**
     * This method returns all the enumeration values that represent additional steps.  We use
     * it to eliminate INIT, which is only permitted on the first step.
     *
     * @return the list of types that are applicable to additional file specs
     */
    public static JoinType[] usefulValues() {
        return Arrays.stream(JoinType.values()).filter(x -> x != INIT).toArray(JoinType[]::new);
    }

}
