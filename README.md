WordSpaceVisualization
======================

The word space visualization tool supports the intuitive comprehension of text mining results. It combines associations from a word space model with the explicit results of an event extraction system. 

Running WSV
-----------

The Java Runtime Environment is needed to execute the program. The JRE can be obtained from http://www.java.com/de/download/. Next, download WordSpaceVisualization.jar and the TMs, WSMs and Visualizations folders. Place them all in the same folder and run 
> "java -jar WordSpaceVisualization.jar"

If you are analyzing large data sets, it might be necessary to increase the heap space. You can do this using the command line parameter -Xmx, e.g.:
> "java -Xmx2g -jar WordSpaceVisualization.jar"

Creating a new visualization
----------------------------

Choose your word space model and your text mining results. Default ones (with results for the proteasome and COPD) are loaded already. For creating and loading your own ones, see the next sections. Choose your parameters:

* Amount clusters: Determines how many clusters should be created on each level. Note that there is always an additional cluster created for terms that are not found in the word space model.
* Entities per cluster: How many entities are allowed in one cluster? If the amount of entities is higher than this number, subclusters are created.
* Minimum evidences: How many evidence sentences need to be there, so that a relation should be considered in the visualization? Increase this number, if you only want well supported relations to be shown.
* Type filter: Which entity types should be included in the visualization? Multiple ones can be included by separating them with a comma. If you want all types, just leave this field empty.
 
Give your visualization a name and push the "Create visualization"-button. This will automatically store your visualization in the Visualizations-folder and display the visualization. Note that this step might take several minutes.


Analyzing your own text mining results
--------------------------------------

The file containing the text mining results must conform to this simple tab- and pipe-separated format:

    Terms:
    <ID1>   <Synonym1>|<Synonym2>|...   <Type>
    <ID2>   <Synonym1>|<Synonym2>|...   <Type>
    ...
    Relations:
    <Type>    <ID1>   <ID2>   "<Evidence sentence1>"|"<Evidence sentence2>"|...    <Link1>|<Link2>|...
    <Type>    <ID1>   <ID2>   "<Evidence sentence>"   <Link>
    ...

The IDs within the relations have to refer to terms defined before. The type of a term, the evidence sentence and the link are optional. If no evidences are given, the evidence score is assumed to be one. If both evidence sentences and links are given, the amount of both has to match. An evidence sentence is the sentence the relation was extracted from. The type of a term can be used to filter the text mining results when creating a new visualization.

Once you have prepared your text mining file, place it in the TMs folder and start the word space visualization tool. Choose the word space model which you want to use for the visualization (a default one is loaded already, alternatively you can create your own (see next section for how to do this)). Push the 'Load text mining results'-button and choose your text mining file. And create a new visualization like described above.

Creating a new word space model
-------------------------------

For creating a new word space model, you need a corpus of text files from which it should be derived. Once you have such a corpus you can start the application, choose a name and method for the creation of the WSM and push the 'Create WSM from corpus'-button. Choose your corpus and the new word space model will be created in the WSMs-folder and will be loaded in the application. The currently available methods are the following:

* LSA: Latent Semantic Analysis
* RI : Random Indexing

Once you have created your WSM, choose your text mining results and create a new visualization like described above.

Searching terms within a visualization
--------------------------------------

When loading a new visualization first the clustered terms occurring in the text mining results are shown. By entering a search term at the top of the tool the semantic visualization centered around this term is shown. Multiple terms can be entered into the search field by separating them with commas. In order to go back to the original cluster, click the 'Visualize'-button without entering any search terms.

Navigating in a graph
---------------------

The following ways of navigation exist:

* Left click cluster: Explore cluster
* Left click term: Show evidences
* Left click edge: Show evidences
* Right click: Move up level
* Drag background: Move graph
* Drag node: Move node
* Mouse wheel: Zoom in/out

