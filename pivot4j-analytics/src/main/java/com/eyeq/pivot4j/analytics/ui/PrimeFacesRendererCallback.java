package com.eyeq.pivot4j.analytics.ui;

import static com.eyeq.pivot4j.ui.CellTypes.AGG_VALUE;
import static com.eyeq.pivot4j.ui.CellTypes.LABEL;
import static com.eyeq.pivot4j.ui.CellTypes.VALUE;
import static com.eyeq.pivot4j.ui.table.TableCellTypes.FILL;
import static com.eyeq.pivot4j.ui.table.TableCellTypes.TITLE;

import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlOutputLink;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.olap4j.Axis;
import org.primefaces.component.column.Column;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.component.panelgrid.PanelGrid;
import org.primefaces.component.row.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eyeq.pivot4j.el.EvaluationFailedException;
import com.eyeq.pivot4j.ui.AbstractRenderCallback;
import com.eyeq.pivot4j.ui.command.UICommand;
import com.eyeq.pivot4j.ui.command.UICommandParameters;
import com.eyeq.pivot4j.ui.table.TableRenderCallback;
import com.eyeq.pivot4j.ui.table.TableRenderContext;
import com.eyeq.pivot4j.util.CssWriter;
import com.eyeq.pivot4j.util.RenderPropertyUtils;

public class PrimeFacesRendererCallback extends
		AbstractRenderCallback<TableRenderContext> implements
		TableRenderCallback {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Map<String, String> iconMap;

	private PanelGrid component;

	private PanelGrid filterComponent;

	private FacesContext facesContext;

	private ExpressionFactory expressionFactory;

	private HtmlPanelGroup header;

	private Row row;

	private Column column;

	private int commandIndex = 0;

	/**
	 * @param facesContext
	 */
	public PrimeFacesRendererCallback(FacesContext facesContext) {
		this.facesContext = facesContext;

		if (facesContext != null) {
			Application application = facesContext.getApplication();

			this.expressionFactory = application.getExpressionFactory();
		}

		// Map command mode names to jQuery's predefined icon names. It can be
		// also done by CSS.
		this.iconMap = new HashMap<String, String>();

		iconMap.put("expandPosition-position", "ui-icon-plus");
		iconMap.put("collapsePosition-position", "ui-icon-minus");
		iconMap.put("expandMember-member", "ui-icon-plusthick");
		iconMap.put("collapseMember-member", "ui-icon-minusthick");
		iconMap.put("drillDown-replace", "ui-icon-arrowthick-1-e");
		iconMap.put("drillUp-replace", "ui-icon-arrowthick-1-n");
		iconMap.put("sort-basic-natural", "ui-icon-triangle-2-n-s");
		iconMap.put("sort-basic-other-up", "ui-icon-triangle-1-n");
		iconMap.put("sort-basic-other-down", "ui-icon-triangle-1-s");
		iconMap.put("sort-basic-current-up", "ui-icon-circle-triangle-n");
		iconMap.put("sort-basic-current-down", "ui-icon-circle-triangle-s");
		iconMap.put("drillThrough", "ui-icon-search");
	}

	/**
	 * @return the parent JSF component
	 */
	public PanelGrid getComponent() {
		return component;
	}

	/**
	 * @param component
	 */
	public void setComponent(PanelGrid component) {
		this.component = component;
	}

	/**
	 * @return filterComponent
	 */
	public PanelGrid getFilterComponent() {
		return filterComponent;
	}

	/**
	 * @param filterComponent
	 */
	public void setFilterComponent(PanelGrid filterComponent) {
		this.filterComponent = filterComponent;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.RenderCallback#getContentType()
	 */
	@Override
	public String getContentType() {
		return null;
	}

	/**
	 * @return the logger
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.AbstractRenderCallback#startRender(com.eyeq.pivot4j.ui.RenderContext)
	 */
	@Override
	public void startRender(TableRenderContext context) {
		super.startRender(context);

		ResourceBundle resources = facesContext.getApplication()
				.getResourceBundle(facesContext, "msg");
		context.setResourceBundle(resources);

		getRenderPropertyUtils().setSuppressErrors(true);

		this.commandIndex = 0;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#startTable(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void startTable(TableRenderContext context) {
		if (context.getAxis() == Axis.FILTER) {
			filterComponent.getChildren().clear();
		} else {
			component.getChildren().clear();
		}
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#startHeader(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void startHeader(TableRenderContext context) {
		this.header = new HtmlPanelGroup();
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#endHeader(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void endHeader(TableRenderContext context) {
		if (context.getAxis() != Axis.FILTER) {
			component.getFacets().put("header", header);
		}

		this.header = null;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#startBody(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void startBody(TableRenderContext context) {
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#startRow(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void startRow(TableRenderContext context) {
		this.row = new Row();
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#startCell(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void startCell(TableRenderContext context) {
		this.column = new Column();

		String id = "col-" + column.hashCode();

		column.setId(id);
		column.setColspan(context.getColumnSpan());
		column.setRowspan(context.getRowSpan());

		RenderPropertyUtils propertyUtils = getRenderPropertyUtils();

		String propertyCategory = context.getRenderPropertyCategory();

		StringWriter writer = new StringWriter();
		CssWriter cssWriter = new CssWriter(writer);

		String type = context.getCellType();

		if (type.equals(LABEL) && !context.getRenderer().getShowParentMembers()
				&& context.getMember() != null) {
			int padding = context.getMember().getDepth() * 10;
			cssWriter.writeStyle("padding-left", padding + "px");
		}

		String fgColor = propertyUtils.getString("fgColor", propertyCategory,
				null);

		if (fgColor != null) {
			cssWriter.writeStyle("color", fgColor);
		}

		String bgColor = propertyUtils.getString("bgColor", propertyCategory,
				null);

		if (bgColor != null) {
			cssWriter.writeStyle("background-color", bgColor);
			cssWriter.writeStyle("background-image", "none");
		}

		String fontFamily = propertyUtils.getString("fontFamily",
				propertyCategory, null);

		if (fontFamily != null) {
			cssWriter.writeStyle("font-family", fontFamily);
		}

		String fontSize = propertyUtils.getString("fontSize", propertyCategory,
				null);

		if (fontSize != null) {
			cssWriter.writeStyle("font-size", fontSize);
		}

		String fontStyle = propertyUtils.getString("fontStyle",
				propertyCategory, null);

		if (fontStyle != null) {
			if (fontStyle.contains("bold")) {
				cssWriter.writeStyle("font-weight", "bold");
			}

			if (fontStyle.contains("italic")) {
				cssWriter.writeStyle("font-style", "oblique");
			}
		}

		writer.flush();

		IOUtils.closeQuietly(writer);

		String style = writer.toString();

		if (StringUtils.isNotEmpty(style)) {
			column.setStyle(style);
		}

		String styleClass = getStyleClass(context);
		String styleClassProperty = propertyUtils.getString("styleClass",
				propertyCategory, null);

		if (styleClassProperty != null) {
			if (styleClass == null) {
				styleClass = styleClassProperty;
			} else {
				styleClass += " " + styleClassProperty;
			}
		}

		column.setStyleClass(styleClass);
	}

	/**
	 * @param context
	 * @return
	 */
	protected String getStyleClass(TableRenderContext context) {
		String styleClass = null;

		String type = context.getCellType();

		if (type.equals(LABEL) || type.equals(TITLE) || type.equals(FILL)) {
			if (context.getAxis() == Axis.COLUMNS) {
				styleClass = "col-hdr-cell";
			} else {
				if (type.equals(LABEL)) {
					styleClass = "row-hdr-cell ui-widget-header";
				} else {
					styleClass = "ui-widget-header";
				}
			}
		} else if (type.equals(AGG_VALUE)) {
			if (context.getAxis() == Axis.ROWS) {
				styleClass = "ui-widget-header ";
			} else {
				styleClass = "";
			}

			styleClass += "agg-title";
		} else if (type.equals(VALUE)) {
			if (context.getAggregator() == null) {
				// PrimeFaces' Row class doesn't have the styleClass property.
				if (context.getRowIndex() % 2 == 0) {
					styleClass = "value-cell cell-even";
				} else {
					styleClass = "value-cell cell-odd";
				}
			} else {
				styleClass = "ui-widget-header agg-cell";

				if (context.getAxis() == Axis.COLUMNS) {
					styleClass += " col-agg-cell";
				} else if (context.getAxis() == Axis.ROWS) {
					styleClass += " row-agg-cell";
				}
			}
		}

		return styleClass;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.RenderCallback#renderCommands(com.eyeq.pivot4j.ui.RenderContext,
	 *      java.util.List)
	 */
	@Override
	public void renderCommands(TableRenderContext context,
			List<UICommand<?>> commands) {
		if (expressionFactory != null) {
			for (UICommand<?> command : commands) {
				UICommandParameters parameters = command
						.createParameters(context);

				CommandButton button = new CommandButton();

				// JSF requires an unique id for command components.
				button.setId("btn-" + commandIndex++);

				button.setTitle(command.getDescription());

				String icon = null;

				String mode = command.getMode(context);
				if (mode == null) {
					icon = iconMap.get(command.getName());
				} else {
					icon = iconMap.get(command.getName() + "-" + mode);
				}

				button.setIcon(icon);

				MethodExpression expression = expressionFactory
						.createMethodExpression(facesContext.getELContext(),
								"#{pivotGridHandler.executeCommand}",
								Void.class, new Class<?>[0]);
				button.setActionExpression(expression);
				button.setUpdate(":grid-form,:editor-form:mdx-editor,:editor-form:editor-toolbar,:source-tree-form,:target-tree-form");
				button.setOncomplete("onViewChanged()");

				UIParameter commandParam = new UIParameter();
				commandParam.setName("command");
				commandParam.setValue(command.getName());
				button.getChildren().add(commandParam);

				UIParameter axisParam = new UIParameter();
				axisParam.setName("axis");
				axisParam.setValue(parameters.getAxisOrdinal());
				button.getChildren().add(axisParam);

				UIParameter positionParam = new UIParameter();
				positionParam.setName("position");
				positionParam.setValue(parameters.getPositionOrdinal());
				button.getChildren().add(positionParam);

				UIParameter memberParam = new UIParameter();
				memberParam.setName("member");
				memberParam.setValue(parameters.getMemberOrdinal());
				button.getChildren().add(memberParam);

				UIParameter hierarchyParam = new UIParameter();
				hierarchyParam.setName("hierarchy");
				hierarchyParam.setValue(parameters.getHierarchyOrdinal());
				button.getChildren().add(hierarchyParam);

				UIParameter cellParam = new UIParameter();
				cellParam.setName("cell");
				cellParam.setValue(parameters.getCellOrdinal());
				button.getChildren().add(cellParam);

				column.getChildren().add(button);
			}
		}
	}

	/**
	 * @see com.eyeq.pivot4j.ui.RenderCallback#renderContent(com.eyeq.pivot4j.ui.RenderContext,
	 *      java.lang.String)
	 */
	@Override
	public void renderContent(TableRenderContext context, String label) {
		context.getExpressionContext().put("label", label);

		String labelText;

		RenderPropertyUtils propertyUtils = getRenderPropertyUtils();

		try {
			labelText = StringUtils.defaultIfEmpty(
					propertyUtils.getString("label",
							context.getRenderPropertyCategory(), label), "");
		} finally {
			context.getExpressionContext().remove("label");
		}

		HtmlOutputText text = new HtmlOutputText();
		String id = "txt-" + text.hashCode();

		text.setId(id);
		text.setValue(labelText);

		String link = propertyUtils.getString("link",
				context.getRenderPropertyCategory(), null);

		if (link == null) {
			column.getChildren().add(text);
		} else {
			HtmlOutputLink anchor = new HtmlOutputLink();
			anchor.setValue(link);
			anchor.getChildren().add(text);

			column.getChildren().add(anchor);
		}
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#endCell(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void endCell(TableRenderContext context) {
		row.getChildren().add(column);
		this.column = null;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#endRow(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void endRow(TableRenderContext context) {
		if (header == null) {
			UIComponent parent = context.getAxis() == Axis.FILTER ? filterComponent
					: component;
			parent.getChildren().add(row);
		} else {
			header.getChildren().add(row);
		}

		this.row = null;
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#endBody(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void endBody(TableRenderContext context) {
	}

	/**
	 * @see com.eyeq.pivot4j.ui.table.TableRenderCallback#endTable(com.eyeq.pivot4j.ui.table.TableRenderContext)
	 */
	@Override
	public void endTable(TableRenderContext context) {
	}

	/**
	 * @see com.eyeq.pivot4j.ui.RenderCallback#endRender(com.eyeq.pivot4j.ui.RenderContext)
	 */
	@Override
	public void endRender(TableRenderContext context) {
		ResourceBundle resources = context.getResourceBundle();

		MessageFormat mf = new MessageFormat(
				resources.getString("error.property.expression.title"));

		// In order not to bombard users with similar error messages.
		for (String category : context.getRenderProperties().keySet()) {
			Map<String, EvaluationFailedException> errors = getRenderPropertyUtils()
					.getLastErrors(category);

			for (String property : errors.keySet()) {
				String title = mf.format(new String[] { resources
						.getString("properties." + property) });

				EvaluationFailedException e = errors.get(property);

				facesContext.addMessage(null, new FacesMessage(
						FacesMessage.SEVERITY_ERROR, title, e.getMessage()));

				if (logger.isWarnEnabled()) {
					logger.warn(title, e);
				}
			}
		}

		this.commandIndex = 0;

		super.endRender(context);
	}
}
