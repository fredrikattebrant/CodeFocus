package codefocus.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.ui.model.application.ui.MContext;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindowElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CodeFocusHandler extends AbstractHandler {
	public static String RENDERER_URI = "bundleclass://CodeFocus/codefocus.renderers.CodeFocusDWRenderer"; //$NON-NLS-1$

	private IWorkbenchWindow window;
	
	private EModelService ms;
	private EPartService ps;
	private MTrimmedWindow winModel;

	private MPerspective curPersp;
	
	/**
	 * The constructor.
	 */
	public CodeFocusHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	private static final String MINIMIZED_AND_SHOWING = "MinimizzedAndShowing"; //$NON-NLS-1$
	public Object execute(ExecutionEvent event) throws ExecutionException {
		window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		winModel = (MTrimmedWindow) window.getService(MTrimmedWindow.class);
		ms = winModel.getContext().get(EModelService.class);
		ps = winModel.getContext().get(EPartService.class);

		closeOpenMinimizedStacks();
		
		curPersp = ms.getActivePerspective(winModel);
		String childId = curPersp.getChildren().get(0).getElementId();
		if (childId == null || !childId.equals("CFPlaceholder")) {
			enableCoseFocus();
		} else {
			disableCodeFocus();
		}
		return null;
	}
	
	private void closeOpenMinimizedStacks() {
		if (ms != null) {
			List<MToolControl> tcList = ms.findElements(winModel, null, MToolControl.class, Arrays.asList(MINIMIZED_AND_SHOWING));
			for (MToolControl tc : tcList) {
				tc.getTags().remove(MINIMIZED_AND_SHOWING);
			}
		}
	}

	private void enableCoseFocus() {
		ps.activate(null);
		
		MTrimmedWindow dw = ms.createModelElement(MTrimmedWindow.class);
		dw.getPersistedState().put(IPresentationEngine.STYLE_OVERRIDE_KEY, Integer.toString(0));
		//dw.getTags().add(IPresentationEngine.WINDOW_MAXIMIZED_TAG);
		dw.setElementId("Code Focus");
		
		// Explicitly set the window's bounds
		Rectangle displayBounds = window.getShell().getDisplay().getClientArea();
		dw.setX(displayBounds.x);
		dw.setY(displayBounds.y);
		dw.setWidth(displayBounds.width);
		dw.setHeight(displayBounds.height);

		MUIElement perspRoot = curPersp.getChildren().get(0);
		MPartSashContainer psc = ms.createModelElement(MPartSashContainer.class);
		psc.setElementId("CFPlaceholder");
		curPersp.getChildren().add(psc);
		curPersp.getChildren().remove(perspRoot);
		dw.getChildren().add((MWindowElement) perspRoot);
		
		transferTrimStacks(winModel, dw);
		
		curPersp.getWindows().add(dw);
		transferContexts(curPersp.getContext(), dw.getContext(), perspRoot);
		
		ps.requestActivation();
	}

	private void transferContexts(IEclipseContext from,
			IEclipseContext to, MUIElement toSearch) {
		List<MContext> contexts = ms.findElements(toSearch, null, MContext.class, null);
		for (MContext context : contexts) {
			if (context.getContext() == null)
				continue;
			IEclipseContext ec = context.getContext();
			if (ec.getParent() == from) {
				//System.out.println("TC: " + ((MUIElement)context).getElementId());
				ec.setParent(to);
			}
		}
	}

	private void transferTrimStacks(MTrimmedWindow from, MTrimmedWindow to) {
		List<MToolControl> trimStacks = ms.findElements(from, null, MToolControl.class, Arrays.asList("TrimStack"));
		List<MToolControl> trimStacksToTransfer = new ArrayList<MToolControl>();
		for (MToolControl tc : trimStacks) {
			if (tc.getElementId().contains(curPersp.getElementId())) {
				trimStacksToTransfer.add(tc);
			}
		}
		
		for (MToolControl tc : trimStacksToTransfer) {
			MUIElement trimElement = tc.getParent();
			if (trimElement instanceof MTrimBar) {
				MTrimBar trimBar = (MTrimBar) trimElement;
				MTrimBar dwTrim = ms.getTrim(to, trimBar.getSide());
				dwTrim.getChildren().add(tc);
				
				// Restore the TBR of the trim the elements are being added to
				if (trimBar.isToBeRendered())
					dwTrim.setToBeRendered(true);
			}			
		}
	}

	private void disableCodeFocus() {
		ps.activate(null);
		
		MTrimmedWindow dw = (MTrimmedWindow) ms.find("Code Focus", curPersp);
		transferTrimStacks(dw, winModel);

		MUIElement placeholder = ms.find("CFPlaceholder", curPersp);
		MWindowElement dwRoot = dw.getChildren().get(0);
		dw.getChildren().remove(dwRoot);
		curPersp.getChildren().add((MPartSashContainerElement) dwRoot);
		
		placeholder.setToBeRendered(false);
		curPersp.getChildren().remove(placeholder);

		transferContexts(dw.getContext(), curPersp.getContext(), dwRoot);
		dw.setToBeRendered(false);
		curPersp.getWindows().remove(dw);
		
		ps.requestActivation();
	}
}
