package import_catalogue;

import java.io.File;

import org.eclipse.swt.widgets.Event;

import catalogue.Catalogue;
import catalogue_generator.ThreadFinishedListener;
import import_catalogue.CatalogueImporter.ImportFileFormat;
import ui_progress_bar.FormProgressBar;
import ui_progress_bar.ProgressListener;
import ui_progress_bar.ProgressStep;

/**
 * Thread used to import a catalogue from three different formats:
 * .ecf, .xml and .xlsx. Note that the real import process involves
 * only the .xlsx file. If an .ecf or an .xml file are used, they are
 * first converted to an .xlsx file to import it.
 * Import pipeline:
 * .ecf => .xml => .xlsx => import .xlsx
 * @author avonva
 *
 */
public class CatalogueImporterThread extends Thread {

	private Catalogue openedCat;
	private String filename;  // path of the file
	private ImportFileFormat format;  // the format of the file
	
	// called when import is finished
	private ThreadFinishedListener doneListener;
	
	// progress bar used to notify the user
	private FormProgressBar progressBar;
	private int maxProgress = 100;
	
	/**
	 * Initialize the import thread
	 * @param filename path of the file we want to import
	 * @param format in which format is the file that we want to import
	 */
	public CatalogueImporterThread( String filename, 
			ImportFileFormat format ) {
		this.filename = filename;
		this.format = format;
	}
	
	public CatalogueImporterThread( File file, 
			ImportFileFormat format ) {
		this ( file.getAbsolutePath(), format );
	}
	
	/**
	 * Run the import thread
	 */
	public void run () {

		ProgressListener listener = new ProgressListener() {
			
			@Override
			public void progressStepStarted(ProgressStep step) {
				
				if ( step != null && progressBar != null )
					progressBar.setLabel( step.getName() );
			}
			
			@Override
			public void progressChanged( ProgressStep step, 
					double addProgress, int maxProgress) {

				// update progress bar
				if ( progressBar != null )
					progressBar.addProgress( addProgress );
			}

			@Override
			public void failed(ProgressStep step) {}


		};
		
		CatalogueImporter importer = new CatalogueImporter(filename, format, 
				listener, maxProgress );
		
		importer.setOpenedCat( openedCat );
		importer.makeImport();
		
		handleDone();
	}


	/**
	 * Call the done listener if it was set
	 * Pass as data the xlsx filename
	 */
	private void handleDone() {

		// end process
		if ( progressBar != null )
			progressBar.close();
		
		if ( doneListener != null ) {
			Event event = new Event();
			event.data = filename;
			doneListener.finished( this, ThreadFinishedListener.OK );
		}
	}
	
	/**
	 * Called when all the operations are finished
	 * @param doneListener
	 */
	public void addDoneListener ( ThreadFinishedListener doneListener ) {
		this.doneListener = doneListener;
	}
	
	/**
	 * Set the progress bar for the thread
	 * @param progressForm
	 */
	public void setProgressBar( FormProgressBar progressBar, int maxProgress ) {
		this.progressBar = progressBar;
		this.maxProgress = maxProgress;
	}
	
	/**
	 * Set the progress bar for the thread
	 * @param progressForm
	 */
	public void setProgressBar( FormProgressBar progressBar ) {
		this.progressBar = progressBar;
		this.maxProgress = 100;
	}
	
	/**
	 * If we are importing a workbook into an opened catalogue
	 * we need to specify which is the catalogue, otherwise
	 * we will get errors in the import process due to the wrong
	 * db path of the catalogue (which is determined by the
	 * catalogue code + version)
	 * @param localCat
	 */
	public void setOpenedCatalogue( Catalogue openedCat ) {
		this.openedCat = openedCat;
	}
}
