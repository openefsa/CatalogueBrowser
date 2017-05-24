package ui_main_panel;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue_object.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import dcf_webservice.ReserveLevel;
import global_manager.GlobalManager;
import messages.Messages;
import session_manager.RestoreableWindow;
import session_manager.WindowPreference;
import ui_main_menu.FileMenu;
import ui_main_menu.LoginMenu;
import ui_main_menu.MainMenu;
import ui_main_menu.MenuListener;
import ui_main_menu.ToolsMenu;
import ui_main_menu.ViewMenu;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import ui_search_bar.SearchEvent;
import ui_search_bar.SearchListener;
import ui_search_bar.SearchPanel;
import user_preferences.UIPreference;
import utilities.GlobalUtil;

/**
 * Main UI class, it displays the main page of the browser. Here we have the main tree viewer, the term properties (
 * names, scopenotes, implicit attributes, implicit facets, applicabilities...), a search bar, a combo box to select
 * the current hierarchy or facet list...
 * 
 * @author Thomas Milani(documentation), Valentino Avon
 * @version 0.2.1
 */
public class MainPanel implements Observer, RestoreableWindow {
	
	// code for saving window dimensions in db
	private final static String WINDOW_CODE = "MainPanel";

	// the shell which hosts the UI
	public Shell shell;

	// main menu (upper left menu)
	private MainMenu menu;
	
	// search bar and table
	private SearchPanel searchPanel;
	
	// label which shows the current open catalogue label
	private CatalogueLabel catalogueLabel;
	
	// combo box with radio buttons to select the displayed hierarchy
	private HierarchySelector hierarchySelector;
	
	// checkboxes to filter the tree terms
	private TermFilter termFilter;
	
	// main tree which allows browsing catalogue terms
	private TermsTreePanel tree;

	// term properties in three tabs
	private TermPropertiesPanel tabPanel;

	/**
	 * Initialize the main UI panel
	 * @param shell
	 */
	public MainPanel( Shell shell ) {
		this.shell = shell;
	}
	
	@Override
	public String getWindowCode() {
		return WINDOW_CODE;
	}
	
	@Override
	public Shell getWindowShell() {
		return shell;
	}
	
	/**
	 * Creates the user interface
	 */
	public void initGraphics () {

		// add the main menu to the shell
		addMainMenu( shell );

		// add all the swt widgets to the main UI
		addWidgets ( shell );
		
		// shell name, image, window dimensions (based on
		// widget! Need to call it after addWidgets)
		setShellGraphics();
	}
	
	/**
	 * Refresh the catalogue label and the other components
	 * @param catalogue
	 */
	public void refresh ( final Catalogue catalogue ) {
		
		// redraw menus in the ui thread ( we use async exec since
		// this method is potentially called by threads not in the UI )
		Display.getDefault().asyncExec( new Runnable() {

			@Override
			public void run() {

				catalogueLabel.setText( catalogue );

				refresh();
				
				Display.getDefault().update();

				try {
					Thread.sleep( 100 );
				} catch ( InterruptedException e ) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Method for Refreshing GUI. Refresh Search, tree and(in case of not
	 * ReadOnly) corex flag and state flag; folder and shell redraw; shell
	 * update and layout
	 */
	public void refresh () {
		System.out.println( "Refreshing ssss" );
		// redraw menus in the ui thread ( we use async exec since
		// this method is potentially called by threads not in the UI )
		Display.getDefault().asyncExec( new Runnable() {

			@Override
			public void run() {
				
				// redraw the main menu to refresh buttons
				shell.setMenuBar( menu.createMainMenu() );
				menu.refresh();

				// redraw the tree menu to refresh buttons
				tree.addContextualMenu( true );
				
				tabPanel.refresh();
				tabPanel.redraw();
				
				catalogueLabel.refresh();
				
				searchPanel.refresh( true );
				//tree.refresh( true );
				
				shell.redraw();
				shell.update();
				shell.layout();
				
				Display.getDefault().update();
				
				try {
					Thread.sleep( 100 );
				} catch ( InterruptedException e ) {
					e.printStackTrace();
				}
			}
		});
	}
	

	/**
	 * Set the name and the image of the shell. Moreover, set the layout and the paint listener
	 */
	private void setShellGraphics () {

		/* use layout manager */
		shell.setLayout( new GridLayout( 1 , false ) );
		
		shell.addPaintListener( new PaintListener() {
			public void paintControl ( PaintEvent e ) {
				shell.layout();
			}
		} );
		
		shell.setMaximized( true );
		
		// restore the old dimensions of the window
		WindowPreference.restore( this );
		
		// save this window dimensions when it is closed
		WindowPreference.saveOnClosure( this );
	}

	/**
	 * Change the hierarchy to be visualized and expand the tree to the selected term
	 * @param hierarchy the new hierarchy to be visualized
	 * @param term the selected term which has to be reopened in the new hierarchy
	 */
	public void changeHierarchy ( Hierarchy hierarchy, Nameable term ) {

		changeHierarchy ( hierarchy );
		
		// select the term in the tree
		tree.selectTerm( term );
		
		// refresh the browser
		refresh();
	}
	
	/**
	 * Change the hierarchy to be visualized
	 * @param hierarchy the new hierarchy to be visualized
	 */
	public void changeHierarchy ( Hierarchy hierarchy ) {
		
		// update the combo box selection
		hierarchySelector.setSelection( hierarchy );
	}
	
	
	/**
	 * Enable or disable the entire user interface
	 */
	private void enableUI ( boolean enable ) {
		
		// enable the combo box for hierarchies
		hierarchySelector.setEnabled( enable );
		
		// enable search bar
		searchPanel.setEnabled( enable );
		
		// enable display filters
		termFilter.setEnabled( enable );
	}
	
	/**
	 * Load all the default graphics input and listeners
	 */
	private void loadData( Catalogue catalogue ) {
		
		// update the tree input and set its contextual menu
		// set the tree input

		tree.addUpdateListener( new Listener() {

			@Override
			public void handleEvent(Event event) {
				tabPanel.refresh();
			}
		});
		
		// refresh applicabilities when drop finishes
		tree.addDropListener( new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				tabPanel.refresh();
			}
		});
		
		// if "see in other hierarchies" was pressed
		tree.addChangeHierarchyListener( new HierarchyChangedListener() {
			
			@Override
			public void hierarchyChanged( HierarchyEvent event ) {
				
				// update the hierarchy to be shown in the main pane
				changeHierarchy( event.getHierarchy(), (Term) event.getTerm() );
			}
		});		

		// set the hierarchy combo box input and select the first available hierarchy
		hierarchySelector.setInput( catalogue.getHierarchies() );

		// get the selected hierarchy of the combo box and make it the current one
		Hierarchy currentHierarchy = hierarchySelector.getSelectedHierarchy();

		tree.setInput( currentHierarchy );
	}
	
	/**
	 * Remove all the input from the graphics
	 */
	private void removeData() {

		// remove elements from the tree
		tree.setInput( null );

		// remove the term from the panel
		tabPanel.setTerm( null );
		
		// remove input from tabs
		tabPanel.resetInput();
		
		// disable tabs
		tabPanel.setEnabled( false );

		// disable search
		searchPanel.setEnabled( false );

		// clear search results
		searchPanel.removeAll();

		hierarchySelector.resetGraphics();
		hierarchySelector.setEnabled( false );
	}
	
	/**
	 * Add all the widgets to the main UI
	 * @param parent
	 */
	private void addWidgets ( Composite parent ) {

		// I add a sashForm which is a split pane
		SashForm sashForm = new SashForm( shell , SWT.HORIZONTAL );
		GridData shellGridData = new GridData();
		shellGridData.horizontalAlignment = SWT.FILL;
		shellGridData.verticalAlignment = SWT.FILL;
		shellGridData.grabExcessHorizontalSpace = true;
		shellGridData.grabExcessVerticalSpace = true;
		sashForm.setLayoutData( shellGridData );
		
		// left group for catalogue label, search bar and table
		GridData leftData = new GridData();
		leftData.minimumWidth = 180;
		leftData.widthHint = 180;
		
		Composite leftGroup = new Composite( sashForm , SWT.NONE );
		leftGroup.setLayout( new GridLayout( 1 , false ) );
		leftGroup.setLayoutData( leftData );

		// add the label which displays the catalogue label
		addCatalogueLabel ( leftGroup );

		// add the search bar and table
		addSearchPanel ( leftGroup );

		// group which contains hierarchy selector, tree viewer and tab folder
		Group rightGroup = new Group( sashForm , SWT.NONE );
		rightGroup.setLayout( new GridLayout( 1 , false ) );

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

		rightGroup.setLayoutData( gridData );

		// add hierarchy selector and deprecated/non reportable filters
		addDisplayFilters( rightGroup );

		// add tree viewer and term tabs
		addRightSashForm( rightGroup );
		
		// make the tree viewer observer of the selected hierarchy
		hierarchySelector.addObserver( tree );
		
		// make implicit facet tab observer of selected hierarchy
		hierarchySelector.addObserver( tabPanel );
		
		// make the search results table aware of the current hierarchy
		hierarchySelector.addObserver( searchPanel );
		
		// make the tree observer of the checked filters
		termFilter.addObserver( tree );
		
		// objects which observe global manager current catalogue
		GlobalManager.getInstance().addObserver( hierarchySelector );
		GlobalManager.getInstance().addObserver( searchPanel );
		GlobalManager.getInstance().addObserver( tree );
		GlobalManager.getInstance().addObserver( catalogueLabel );
		GlobalManager.getInstance().addObserver( menu );
		GlobalManager.getInstance().addObserver( tabPanel );
		GlobalManager.getInstance().addObserver( this );
		
		// tab panel listen term changes
		tree.addObserver( tabPanel );
		
		// add the tree as observer of the term filter
		// and restore the term filter status to the
		// previous one
		termFilter.addObserver( tree );
		termFilter.restoreStatus();
		
		// set the weights once all the widgets are inserted
		sashForm.setWeights( new int[] { 1, 4 } );
	}

	
	/**
	 * Add the main menu to the shell
	 * @param shell
	 */
	private void addMainMenu ( final Shell shell ) {
		
		// create the main menu and set its listener for some buttons
		menu = new MainMenu( shell );

		menu.setFileListener( new MenuListener() {
			
			@Override
			public void buttonPressed( MenuItem button, int code, Event event ) {
				
				switch ( code ) {
				case FileMenu.OPEN_CAT_MI:
					
					removeData();
					
					refresh();
					
					// get the selected catalogue
					Catalogue catalogue = (Catalogue) event.data;

					// enable the user interface only if we have data in the current catalogue
					if ( !catalogue.isEmpty() ) {
						enableUI( true );
						loadData( catalogue );
					}
					
					break;
					
				case FileMenu.CLOSE_CAT_MI:
					
					removeData();
					enableUI( false );
					refresh();
					
					break;
				
				default:
					break;
				}
			}
		});
		
		menu.setViewListener( new MenuListener() {
			
			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {

				switch ( code ) {
				case ViewMenu.EXPAND_MI:
					tree.expandSelectedTerms( TreeViewer.ALL_LEVELS );
					break;
				case ViewMenu.COLLAPSE_NODE_MI:
					tree.collapseSelectedTerms( TreeViewer.ALL_LEVELS );
					break;
				case ViewMenu.COLLAPSE_TREE_MI:
					tree.collapseAll();
					break;
				default:
					break;
				}
			}
		});
		
		menu.setToolsListener( new MenuListener() {
			
			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {
				
				switch ( code ) {
				
				case ToolsMenu.IMPORT_CAT_MI:
					
					// get the current catalogue
					Catalogue catalogue = GlobalManager.getInstance()
							.getCurrentCatalogue();

					// enable user interface if the catalogue is not empty
					if ( catalogue != null && !catalogue.isEmpty() ) {
						enableUI( true );
						loadData( catalogue );
					}
					
					break;
					
				case ToolsMenu.ATTR_EDITOR_MI:
					
					// refresh
					tabPanel.setTerm( tree.getFirstSelectedTerm() );
					refresh();
					
					break;
					
				default:
					break;
				}
			}
		});
		
		menu.setLoginListener( new MenuListener() {
			
			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {
				switch ( code ) {
				case LoginMenu.LOGIN_MI:
					refresh();
					break;
				default:
					break;
				}
			}
		});

		// initialize the main menu with all the sub menus and menu items
		shell.setMenuBar( menu.createMainMenu() );
		
		// set the main panel as observer of the main menu
		menu.addObserver( this );
	}
	
	
	/**
	 * Add a label which displays the current opened catalogue
	 * @param parent
	 */
	private void addCatalogueLabel ( Composite parent ) {
		catalogueLabel = new CatalogueLabel( parent );
	}
	
	/**
	 * Add the search panel (search bar and search table results)
	 * @param parent
	 */
	private void addSearchPanel ( Composite parent ) {

		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();
		
		// add a search table
		searchPanel = new SearchPanel ( parent, true, catalogue );
		
		// called when a hierarchy is selected in the
		// results table using the contextual menu
		searchPanel.addHierarchyListener( new HierarchyChangedListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent event) {
				changeHierarchy ( event.getHierarchy(), event.getTerm() );
			}
		});
		
		// Set the search listener (actions performed at the end of the search)
		searchPanel.addSearchListener( new SearchListener() {

			@Override
			public void searchPerformed ( SearchEvent event ) {
				
				// if empty warn the user
				if ( event.getResults().isEmpty() ) {

					GlobalUtil.showDialog( shell, 
							Messages.getString("Browser.SearchResultTitle"), 
							Messages.getString("Browser.SearchResultMessage"), SWT.OK );					
				}
			}
		} );

		// expand the selected term of the search results in the tree if clicked
		searchPanel.addSelectionChangedListener( new ISelectionChangedListener() {

			public void selectionChanged ( SelectionChangedEvent event ) {

				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				// return if no selected elements
				if ( selection.isEmpty() )
					return;

				tree.selectTerm( (Nameable) selection.getFirstElement() );
			}
		} );
		
		// set Go as default button
		searchPanel.addDefaultButton( shell );
	}
	
	
	/**
	 * Add the hierarchy selector and check buttons to
	 * filter the tree viewer terms
	 * @param parent
	 */
	private void addDisplayFilters ( Composite parent ) {
		
		Composite selectionGroup = new Composite( parent , SWT.NONE );
		selectionGroup.setLayout( new GridLayout( 10 , false ) );

		// hierarchy selector (combo box + radio buttons)
		hierarchySelector = new HierarchySelector( selectionGroup );
		hierarchySelector.display();

		// create a filter to filter the tree terms
		// based on their state (deprecated, not reportable)
		termFilter = new TermFilter( selectionGroup );
		termFilter.display( UIPreference.hideDeprMain, 
				UIPreference.hideNotReprMain );
	}

	/**
	 * Add the right sashform, that is, the form which contains the tree
	 * viewer and the tab folder.
	 * @param parent
	 */
	private void addRightSashForm ( Composite parent ) {
		
		SashForm sashForm2 = new SashForm( parent , SWT.HORIZONTAL );
		
		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		sashForm2.setLayoutData( gridData );
		
		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();
		
		// add the main tree viewer
		tree = new TermsTreePanel( sashForm2, catalogue );
		
		// add the tab folder
		addTabFolder( sashForm2 );

		sashForm2.setWeights( new int[] { 5, 3 } );
	}
	
	/**
	 * Add the tab folder
	 * @param parent
	 */
	private void addTabFolder ( Composite parent ) {
		
		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();
		
		// initialize tab panel
		tabPanel = new TermPropertiesPanel( parent, catalogue );
		
		// add the open listener, if we open an applicability
		// we move the hierarchy to the selected one
		tabPanel.addOpenListener( new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged( HierarchyEvent event) {

				// get the selected hierarchy from the event
				Hierarchy selectedHierarchy = event.getHierarchy();
				Nameable parent = event.getTerm();

				// change the hierarchy, show term if term selected
				// otherwise just open the hierarchy
				if ( parent instanceof Term )
					changeHierarchy( selectedHierarchy, parent );
				else
					changeHierarchy( selectedHierarchy );
			}
		});

		// set the add listener, if we add an applicability
		// refresh the implicit facet tab (the inherited facets changes)
		tabPanel.addAddListener( new Listener() {

			@Override
			public void handleEvent(Event event) {
				tree.refresh();
			}
		});

		// set the remove listener, if we remove an applicability
		// we refresh the tree and the implicit facet tab
		tabPanel.addRemoveListener( new Listener() {

			@Override
			public void handleEvent(Event event) {
				tree.refresh();
			}
		});

		// add the usage listener, if we change the usage we refresh the tree
		// since non reportable terms become italic
		tabPanel.addUsageListener( new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged( HierarchyEvent event ) {
				tree.refresh();
			}
		});

		// if an object is modified, then we refresh the tree
		tabPanel.addUpdateListener( new Listener() {

			@Override
			public void handleEvent(Event event) {
				Term term = (Term) event.data;
				tree.refresh( term );
			}
		});
	}


	// warned if the reserve level of the current catalogue is changed
	@Override
	public void update(Observable arg0, Object arg1) {

		// refresh ui if the current catalogue changed
		if ( arg0 instanceof GlobalManager ) {
			if ( arg1 instanceof Catalogue ) {
				refresh();
			}
		}
		
		else if ( arg1 instanceof Catalogue ) {
			refresh( (Catalogue) arg1 );
		}
		
		// refresh UI if the current catalogue reserve level was changed
		// or if refresh is required
		else if ( arg1 instanceof ReserveLevel || arg0 instanceof MainMenu ) {
			refresh();
		}
	}
}
