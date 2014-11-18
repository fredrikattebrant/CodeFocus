package codefocus.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.renderers.swt.SWTPartRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.TrimmedPartLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CopyOfCodeFocusHandler extends AbstractHandler {
	private EModelService ms;
	private EPartService ps;
	private IPresentationEngine pe;
	
	private IWorkbenchWindow window;
	private MTrimmedWindow winModel;
	/**
	 * The constructor.
	 */
	public CopyOfCodeFocusHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		winModel = (MTrimmedWindow) window.getService(MTrimmedWindow.class);
		Shell cfShell = (Shell) winModel.getTransientData().get("CodeFocusShell");
		if (cfShell == null)
			enableCoseFocus();
		else
			disableCodeFocus(cfShell);
		return null;
	}
	
	private void enableCoseFocus() {
		TrimmedPartLayout curLayout = (TrimmedPartLayout) window.getShell().getLayout();
		
		Shell fullScreen = new Shell(window.getShell(), SWT.NONE);
		fullScreen.setMaximized(true);
		fullScreen.setLayout(new FillLayout());

		curLayout.clientArea.setParent(fullScreen);
		curLayout.clientArea = null;
		
		fullScreen.layout(true, true);
		fullScreen.setVisible(true);
		
		winModel.getTransientData().put("CodeFocusShell", fullScreen);
	}

	private void disableCodeFocus(Shell cfShell) {
		Shell winShell = window.getShell();
		TrimmedPartLayout curLayout = (TrimmedPartLayout) winShell.getLayout();
		curLayout.clientArea = (Composite) cfShell.getChildren()[0];
		curLayout.clientArea.setParent(winShell);
		
		winShell.layout(true, true);
		cfShell.dispose();
		winModel.getTransientData().remove("CodeFocusShell");
	}
}
