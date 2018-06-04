package application_src.application_model.search.CElegansSearch;


import application_src.application_model.data.CElegansData.CellDeaths.CellDeaths;
import application_src.application_model.data.CElegansData.Connectome.Connectome;
import application_src.application_model.data.CElegansData.Connectome.NeuronalSynapse;
import application_src.application_model.data.CElegansData.Gene.WormBaseQuery;
import application_src.application_model.data.CElegansData.SulstonLineage.SulstonLineage;
import application_src.application_model.data.CElegansData.PartsList.PartsList;
import application_src.application_model.data.OrganismDataType;
import application_src.application_model.search.OrganismSearch;
import javafx.scene.control.TreeItem;

import java.util.*;

import static application_src.application_model.data.CElegansData.PartsList.PartsList.getLineageNamesByFunctionalName;

public class CElegansSearch implements OrganismSearch {


    ////////// LINEAGE SEARCH ////////////////////////////////////////////////////////////////////////////////////
    /**
     *
     * @param searchString
     * @param includeAncestors
     * @param includeDescendants
     * @return
     */
    @Override
    public AbstractMap.SimpleEntry<OrganismDataType, List<String>> executeLineageSearch(String searchString, boolean includeAncestors, boolean includeDescendants) {
        OrganismDataType lineageDataType = OrganismDataType.LINEAGE;
        ArrayList<String> searchResults = new ArrayList<>();

        // access the PartsList and CellDeaths arrays
        ArrayList<String> lineageNames = (ArrayList<String>)PartsList.getLineageNames();
        ArrayList<String> cellDeaths = (ArrayList<String>)CellDeaths.getCellDeathsAsArray();


        // check for exact and prefix match in both lists
        boolean found = false;
        for (String s : lineageNames) {
            if (s.toLowerCase().equals(searchString.toLowerCase()) || s.toLowerCase().startsWith(searchString.toLowerCase())) {
                searchResults = (ArrayList<String>)executeLineageSearch(searchString, s, includeAncestors, includeDescendants);
                found = true;
                break;
            }
        }

        if (!found) {
            for (String s : cellDeaths) {
                if (s.toLowerCase().equals(searchString.toLowerCase()) || s.toLowerCase().startsWith(searchString.toLowerCase())) {
                    searchResults = (ArrayList<String>)executeLineageSearch(searchString, s, includeAncestors, includeDescendants);
                    found = true;
                    break;
                }
            }
        }

        // check in the special cases of the sulston lineage
        if (!found) {
            String[] specialCasesAsStringArray = SulstonLineage.getSpecialCasesAsStringArray();

            for (String s : specialCasesAsStringArray) {
                if (s.toLowerCase().equals(searchString.toLowerCase())) {
                    searchResults = (ArrayList<String>)executeLineageSearch(searchString, s, includeAncestors, includeDescendants);
                }
            }
        }


        AbstractMap.SimpleEntry<OrganismDataType, List<String>> results =
                new AbstractMap.SimpleEntry(lineageDataType, cleanResults(searchResults));
        return results;
    }

    ///// lineage search helper functions /////

    /**
     *
     * @param searchString
     * @param matchString
     * @param includeAncestors
     * @param includeDescendants
     * @return
     */
    private List<String> executeLineageSearch(String searchString, String matchString, boolean includeAncestors, boolean includeDescendants) {
        ArrayList<String> results = new ArrayList<>();

        // all combinations that require search
        if (includeAncestors) {
            /* peel off letters of the search string, and add each one to the results.
             * with each variation of the string, check if it is a special case in the
             * sulston lineage
             */
            TreeItem<String> P0_root = SulstonLineage.getP0_root();
            String[] specialCasesAsStringArray = SulstonLineage.getSpecialCasesAsStringArray();

            for (int i = searchString.length(); i >= 0; i--) {
                String substr = searchString.substring(0, i);
                if (Arrays.asList(specialCasesAsStringArray).contains(substr)) {
                    results.add(substr);
                    results.addAll(addSpecialCasesAncestors(substr));
                    break;
                }

                // add the substring to the results list
                results.add(substr);
            }
        }

        if (includeDescendants) {
            // access the PartsList and CellDeaths arrays
            ArrayList<String> lineageNames = (ArrayList<String>)PartsList.getLineageNames();
            ArrayList<String> cellDeaths = (ArrayList<String>)CellDeaths.getCellDeathsAsArray();

            // first check if this is a special case
            if (Arrays.asList(SulstonLineage.getSpecialCasesAsStringArray()).contains(searchString)) {
                // add all descendants in the special case tree. When a node with no leaf is reached, add all
                // of its descendants using the regular lineage naming paradigm
                TreeItem<String> matchTreeItem = findTreeItem(searchString, SulstonLineage.getP0_root());

                List<TreeItem<String>> children = findChildren(matchTreeItem);
                for (TreeItem<String> child : children) {
                    results.add(child.getValue());
                    if (child.isLeaf()) {
                        results.addAll(addAllDescendants(child.getValue()));
                    }

                }
            }

            // use the standard sulston descendant paradigm to add results (even in the special cases, there are
            // some post-embryonic cells which share the prefix of the special cases, and this will pick them up
            results.addAll(addAllDescendants(searchString));
        }

        // add the search string itself if it's not already there
        if (!results.contains(searchString)) {
            results.add(searchString);
        }

        return results;
    }

    private List<String> addAllDescendants(String searchString) {
        ArrayList<String> results = new ArrayList<>();

        // access the PartsList and CellDeaths arrays
        ArrayList<String> lineageNames = (ArrayList<String>)PartsList.getLineageNames();
        ArrayList<String> cellDeaths = (ArrayList<String>)CellDeaths.getCellDeathsAsArray();

        // for every match found, generate all descendants between the search string and the match
        for (String s : lineageNames) {
            if (s.toLowerCase().startsWith(searchString.toLowerCase())) {
                for (int i = searchString.length()+1; i <= s.length(); i++) {
                    results.add(s.substring(0, i));
                }
            }
        }

        for (String s : cellDeaths) {
            if (s.toLowerCase().startsWith(searchString.toLowerCase())) {
                for (int i = searchString.length()+1; i <= s.length(); i++) {
                    results.add(s.substring(0, i));
                }
            }
        }

        return results;
    }

    /**
     *
     * @param ancestor
     * @return
     */
    private List<String> addSpecialCasesAncestors(String ancestor) {
        ArrayList<String> specialCaseAncestors = new ArrayList<>();

        TreeItem<String> P0_root = SulstonLineage.getP0_root();

        // find the ancestor node
        TreeItem<String> ancestorTreeItem = findTreeItem(ancestor, P0_root);

        // add all ancestors of this item
        while (ancestorTreeItem.getParent() != null) {
            ancestorTreeItem = ancestorTreeItem.getParent();
            specialCaseAncestors.add(ancestorTreeItem.getValue());
        }

        return specialCaseAncestors;
    }

    /**
     *
     * @param queryStr
     * @param root
     * @return
     */
    private TreeItem<String> findTreeItem(String queryStr, TreeItem<String> root) {
        // check if valid
        if (root != null) {
            if (root.getValue().toLowerCase().equals(queryStr.toLowerCase())) {
                return root;
            } else if (root.getChildren().size() != 0) {
                TreeItem<String> foundNode = findTreeItem(queryStr, root.getChildren().get(0)); // 0 for left side child
                if (foundNode == null) {
                    foundNode = findTreeItem(queryStr, root.getChildren().get(1)); // 0 for right side child
                }
                return foundNode;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private List<TreeItem<String>> findChildren(TreeItem<String> root) {
        List<TreeItem<String>> children = new ArrayList<>();
        children = findChildren(children, root);
        return children;
    }

    private List<TreeItem<String>> findChildren(List<TreeItem<String>> children, TreeItem<String> root) {
        if (root == null) {
            return children;
        }

        if (root.isLeaf()) {
            children.add(root);
            return children;
        }

        children = findChildren(children, root.getChildren().get(0));
        children = findChildren(children, root.getChildren().get(1));
        return children;
    }
    ////////// END LINEAGE SEARCH /////////////////////////////////////////////////////////////////////////////////////





    ////////// FUNCTIONAL SEARCH ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public AbstractMap.SimpleEntry<OrganismDataType, List<String>> executeFunctionalSearch(String searchString, boolean includeAncestors, boolean includeDescendants, OrganismDataType intendedResultsType) {
        ArrayList<String> searchResults = new ArrayList<>();

        ArrayList<String> functionalNames = (ArrayList<String>)PartsList.getFunctionalNames();
        for (int i = 0; i < functionalNames.size(); i++) {
            String s = functionalNames.get(i);
            if (s.toLowerCase().startsWith(searchString.toLowerCase())
                    || s.toLowerCase().equals(searchString.toLowerCase())) {
                if (intendedResultsType.equals(OrganismDataType.FUNCTIONAL)) {
                    searchResults.add(s);
                } else if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
                    searchResults.add(PartsList.getLineageNameByIndex(i));
                }
            }
        }


        if (includeAncestors) {
            ArrayList<String> ancestorSearchResults = new ArrayList<>();
            searchResults.stream().forEach(s -> {
                // let's only allow ancestors search on results that are lineage-based, otherwise the
                // results will be mixed format and that's not good for anyone
                if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
                    ancestorSearchResults.addAll(executeLineageSearch(s, true, false).getValue());
                }});
            searchResults.addAll(ancestorSearchResults);
        }

        AbstractMap.SimpleEntry<OrganismDataType, List<String>> results =
                new AbstractMap.SimpleEntry(intendedResultsType, cleanResults(searchResults));
        return results;
    }

    ////////// FUNCTIONAL SEARCH ////////////////////////////////////////////////////////////////////////////////////




    ////////// DESCRIPTION SEARCH ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public AbstractMap.SimpleEntry<OrganismDataType, List<String>> executeDescriptionSearch(String searchString, boolean includeAncestors, boolean includeDescendants, OrganismDataType intendedResultsType) {
        ArrayList<String> searchResults = new ArrayList<>();


        ArrayList<String> descriptions = (ArrayList<String>)PartsList.getDescriptions();
        for (int i = 0; i < descriptions.size(); i++) {
            String d = descriptions.get(i);
            if (d.toLowerCase().contains(searchString.toLowerCase())
                    || d.toLowerCase().equals(searchString.toLowerCase())) {
                if (intendedResultsType.equals(OrganismDataType.FUNCTIONAL)) {
                    searchResults.add(PartsList.getFunctionalNameByIndex(i));
                } else if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
                    searchResults.add(PartsList.getLineageNameByIndex(i));
                }
            }
        }


        if (includeAncestors) {
            ArrayList<String> ancestorSearchResults = new ArrayList<>();
            searchResults.stream().forEach(s -> {
                // let's only allow ancestors search on results that are lineage-based, otherwise the
                // results will be mixed format and that's not good for anyone
                if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
                    ancestorSearchResults.addAll(executeLineageSearch(s, true, false).getValue());
                }});
            searchResults.addAll(ancestorSearchResults);
        }


        AbstractMap.SimpleEntry<OrganismDataType, List<String>> results =
                new AbstractMap.SimpleEntry(intendedResultsType, cleanResults(searchResults));
        return results;
    }

    ////////// END DESCRIPTION SEARCH ////////////////////////////////////////////////////////////////////////////////////


    ////////// CONNECTOME SEARCH ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public AbstractMap.SimpleEntry<OrganismDataType, List<String>> executeConnectomeSearch(String searchString, boolean includeAncestors, boolean includeDescendants, boolean includePresynapticPartners, boolean includePostsynapticPartners, boolean includeElectricalPartners, boolean includeNeuromuscularPartners, OrganismDataType intendedResultsType) {
        ArrayList<String> searchResults = new ArrayList<>();

        searchString = checkQueryCell(searchString);
        // //iterate over synapses
        for (NeuronalSynapse ns : Connectome.getSynapseList()) {
            // check if synapse contains query cell
            if (ns.getCell1().toLowerCase().contains(searchString.toLowerCase())
                    || ns.getCell2().toLowerCase().contains(searchString.toLowerCase())) {

                String cell1 = ns.getCell1();
                String cell2 = ns.getCell2();

                // processUrl type code
                String synapseTypeDescription = ns.getSynapseType().getDescription();

                // find synapse type code for connection, compare to toggle ticks
                switch (synapseTypeDescription) {
                    case Connectome.S_PRESYNAPTIC_DESCRIPTION:
                        if (includePresynapticPartners) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }
                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case Connectome.R_POSTSYNAPTIC_DESCRIPTION:
                        if (includePostsynapticPartners) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }
                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case Connectome.EJ_ELECTRICAL_DESCRIPTION:
                        if (includeElectricalPartners) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }
                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                    case Connectome.NMJ_NEUROMUSCULAR_DESCRPITION:
                        if (includeNeuromuscularPartners) {
                            // don't add duplicates
                            if (!searchResults.contains(cell1)) {
                                searchResults.add(cell1);
                            }
                            if (!searchResults.contains(cell2)) {
                                searchResults.add(cell2);
                            }
                        }
                        break;
                }
            }
        }

        if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
            ArrayList<String> lineageNameResults = new ArrayList<>();
            for (String result : searchResults) {
                lineageNameResults.addAll(PartsList.getLineageNamesByFunctionalName(result));
            }

            ArrayList<String> ancestorSearchResults = new ArrayList<>();
            if (includeAncestors) {
                lineageNameResults.stream().forEach(s -> {
                    // let's only allow ancestors search on results that are lineage-based, otherwise the
                    // results will be mixed format and that's not good for anyone
                    if (intendedResultsType.equals(OrganismDataType.LINEAGE)) {
                        ancestorSearchResults.addAll(executeLineageSearch(s, true, false).getValue());
                    }});
            }
            searchResults.clear();
            searchResults.addAll(lineageNameResults);
            searchResults.addAll(ancestorSearchResults);
        }


        AbstractMap.SimpleEntry<OrganismDataType, List<String>> results =
                new AbstractMap.SimpleEntry(intendedResultsType, cleanResults(searchResults));
        return results;
    }

    /**
     * Retrieves the functional name of an input cell name, whether it is a lineage or functional name.
     *
     * @param searchString
     *         the cell to check
     *
     * @return the functional name of that cell
     */
    public String checkQueryCell(String searchString) {
            if (PartsList.isLineageName(searchString)) {
                searchString = PartsList.getFunctionalNameByLineageName(searchString);
            }
            return searchString;
    }

    ////////// END CONNECTOME SEARCH ////////////////////////////////////////////////////////////////////////////////////


    ////////// GENE SEARCH SEARCH ////////////////////////////////////////////////////////////////////////////////////
    @Override
    public AbstractMap.SimpleEntry<OrganismDataType, List<String>> executeGeneSearch(String searchString, boolean isSearchTermGene, boolean isSearchTermAnatomy, OrganismDataType intendedResultsType) {
        return null;
    }

    ////////// END GENE SEARCH ////////////////////////////////////////////////////////////////////////////////////

    // analogous cells search

    // anatomy search

    // cell deaths search

    // general utilities

    /**
     *
     * @param results
     * @return
     */
    private List<String> cleanResults(List<String> results) {
        // remove duplicates
        Set<String> hs = new HashSet<>();
        hs.addAll(results);
        results.clear();
        results.addAll(hs);

        // remove names with '.' at end
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).endsWith(".")) {
                results.remove(i);
            }

            if (results.get(i).isEmpty()) {
                results.remove(i);
            }

            if (results.get(i).length() <= 1) {
                results.remove(i);
            }
        }

        return results;
    }


    /**
     * unit testing main
     */
    public static void main(String[] args) {
        SulstonLineage.init();
        PartsList.init();
        CellDeaths.init();
        Connectome.init();

        CElegansSearch search = new CElegansSearch();
        AbstractMap.SimpleEntry<OrganismDataType, List<String>> results;

        /////////////// INTERFACE METHODS //////////////////
        // /* LINEAGE TESTING */
        // test exact/prefix lineage match, no ancestors, no descendants
//        results = search.executeLineageSearch("ABar", false, false);
//        System.out.println("Results for lineage search on 'ABar' (a=false, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        // test exact/prefix lineage match, no ancestors, yes descendants
//        results = search.executeLineageSearch("ABplaapa", false, true);
//        System.out.println("Results for lineage search on 'ABplaapa' (a=false, d=true) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        // test exact/prefix lineage match, yes ancestors, no descendants
//        results = search.executeLineageSearch("ABpla", true, false);
//        System.out.println("Results for lineage search on 'ABpla' (a=true, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        // test exact/prefix lineage match, yes ancestors, yes descendants
//        results = search.executeLineageSearch("P3", true, true);
//        System.out.println("Results for lineage search on 'P3' (a=true, d=true) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");



        /* FUNCTIONAL TESTING */
//        results = search.executeFunctionalSearch("AI", false, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for functionall search on 'AI' (a=false, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

//        results = search.executeFunctionalSearch("AIAL", true, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for functionall search on 'AIAL' (a=true, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

//        results = search.executeFunctionalSearch("hyp7", false, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for functionall search on 'hyp7' (a=false, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        /* DESCRIPTION TESTING */
//        results = search.executeDescriptionSearch("Cephalic neurons", true, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for description search on 'Cephalic neurons' (a=true, d=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        /* CONNECTOME TESTING */
//        results = search.executeConnectomeSearch("DA5", false, false, true, false, false, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for connectome search on 'DA5' (a=false, d=false, pre=true, post=false, elec=false, neuromusc=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

//        results = search.executeConnectomeSearch("DA5", false, false, false, true, false, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for connectome search on 'DA5' (a=false, d=false, pre=false, post=true, elec=false, neuromusc=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

//        results = search.executeConnectomeSearch("DA5", false, false, false, false, true, false, OrganismDataType.LINEAGE);
//        System.out.println("Results for connectome search on 'DA5' (a=false, d=false, pre=false, post=true, elec=false, neuromusc=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

//        results = search.executeConnectomeSearch("DA5", false, false, false, false, false, true, OrganismDataType.FUNCTIONAL);
//        System.out.println("Results for connectome search on 'DA5' (a=false, d=false, pre=false, post=true, elec=false, neuromusc=false) -> DataType: " + results.getKey().getDescription());
//        System.out.println("size of results: " + results.getValue().size());
//        for (String s : results.getValue()) {
//            System.out.println(s);
//        }
//        System.out.println("");

        results = search.executeConnectomeSearch("DA5", true, false, true, false, false, true, OrganismDataType.LINEAGE);
        System.out.println("Results for connectome search on 'DA5' (a=false, d=false, pre=false, post=true, elec=false, neuromusc=false) -> DataType: " + results.getKey().getDescription());
        System.out.println("size of results: " + results.getValue().size());
        for (String s : results.getValue()) {
            System.out.println(s);
        }
        System.out.println("");

        /* GENE TESTING */


        //////////// OTHER C ELEGANS METHODS ///////////////
    }
}