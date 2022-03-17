/**
 *
 */
package org.theseed.meta.finders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.theseed.metabolism.MetaModel;
import org.theseed.metabolism.Pathway;
import org.theseed.utils.ParseFailureException;

import com.github.cliftonlabs.json_simple.JsonException;

/**
 * This is the base class for all subsystem-based finders.  The subsystem extension paths are all found
 * and then the best one is chosen by the subclass.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseSubsystemFinder extends PathFinder {

    /** collection of subsystem paths */
    private Collection<Pathway> subsysPaths;

    /**
     * Create this processor.  The main job here is to get the subsystem paths.
     *
     * @param processor		controlling command processor
     *
     * @throws ParseFailureException
     * @throws IOException
     * @throws JsonException
     */
    public BaseSubsystemFinder(IParms processor) throws ParseFailureException, IOException, JsonException {
        super(processor);
        this.subsysPaths = processor.getSubsysPathways();
        if (this.subsysPaths == null) {
            // Here the user cancelled out.
            throw new ParseFailureException("A subsystem is required to do a subsystem path search.");
        } else if (this.subsysPaths.size() < 1) {
            // Here we have a subsystem, but it has no paths in it.
            throw new ParseFailureException("Cannot do a search using an empty subsystem.");
        } else if (this.getNumLeft() < 1) {
            // Here the is no compound in the compound list.
            throw new ParseFailureException("At least one metabolite is required for the search.");
        }

    }

    @Override
    public Pathway computePath() {
        // Put the pathways in a sorted map.  The shortest path found will be placed first, and mapped
        // to the original path.
        var outputs = new HashMap<Pathway, Pathway>(this.subsysPaths.size() * 3 / 2 + 1);
        MetaModel model = this.getModel();
        // Get the first compound.  There is always at least one, because the constructor insists on it.
        String goal1 = this.nextCompound();
        // Form the remaining compounds into a list.  We will reuse the list for each subsystem path.
        List<String> goals = new ArrayList<String>(this.getNumLeft());
        while (this.hasNextCompound())
            goals.add(this.nextCompound());
        // For each subsystem path, we take its output compound and build a path from it.
        for (Pathway subsysPath : subsysPaths) {
            // Get the output of this subsystem path and trace it through the compounds.
            String start = subsysPath.getOutput();
            this.showStatus("Computing path for " + start + ".");
            Pathway outPath = model.getPathway(start, goal1);
            Iterator<String> iter = goals.iterator();
            while (outPath != null && iter.hasNext())
                outPath = model.extendPathway(outPath, iter.next());
            if (outPath != null)
                outputs.put(outPath, subsysPath);
        }
        Pathway retVal = null;
        // Only proceed if we found a path.
        if (outputs.size() > 0) {
            // Pick the shortest output path.
            var paths = this.findBest(outputs);
            // The key here is the output path, and the value is the subsystem path.  Get
            // a copy of the subsystem path and append the output path to it.
            retVal = paths.getValue().clone();
            retVal.append(paths.getKey());
        }
        return retVal;
    }

    /**
     * @return the best pathway for this operation
     *
     * @param outputs	output pathways to choose from
     */
    protected abstract Map.Entry<Pathway, Pathway> findBest(Map<Pathway, Pathway> outputs);

    /**
     * @return the subsystem path collection
     */
    protected Collection<Pathway> getSubsysPaths() {
        return this.subsysPaths;
    }

}
