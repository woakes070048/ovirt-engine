package org.ovirt.engine.ui.webadmin.section.main.view.popup.provider;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.HorizontalSplitTable;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.CheckboxColumn;
import org.ovirt.engine.ui.common.widget.table.column.EditTextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.table.column.ListModelListBoxColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.table.header.CheckboxHeader;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.networks.ImportNetworksModel;
import org.ovirt.engine.ui.uicommonweb.models.providers.ExternalNetwork;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.provider.ImportNetworksPopupPresenterWidget;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

public class ImportNetworksPopupView extends AbstractModelBoundPopupView<ImportNetworksModel> implements ImportNetworksPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ImportNetworksModel, ImportNetworksPopupView> { }

    private final Driver driver = GWT.create(Driver.class);

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ImportNetworksPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface Style extends CssResource {
        String providersStyle();
    }

    @UiField
    Style style;

    @UiField(provided = true)
    @Path(value = "providers.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Provider<?>> providersEditor;

    @UiField(provided = true)
    HorizontalSplitTable<ExternalNetwork> splitTable;

    @Ignore
    EntityModelCellTable<ListModel<ExternalNetwork>> providerNetworks;

    @Ignore
    EntityModelCellTable<ListModel<ExternalNetwork>> importedNetworks;

    private ListModelListBoxColumn<ExternalNetwork, StoragePool> dcColumn;

    @Inject
    public ImportNetworksPopupView(EventBus eventBus, ApplicationResources resources,
            ApplicationConstants constants, ApplicationTemplates templates) {
        super(eventBus, resources);
        // Initialize Editors
        providersEditor = new ListModelListBoxEditor<Provider<?>>(new NullSafeRenderer<Provider<?>>() {

            @Override
            protected String renderNullSafe(Provider<?> provider) {
                return provider.getName();
            }
        });
        providerNetworks = new EntityModelCellTable<ListModel<ExternalNetwork>>(true, false, true);
        importedNetworks = new EntityModelCellTable<ListModel<ExternalNetwork>>(true, false, true);
        splitTable =
                new HorizontalSplitTable<ExternalNetwork>(providerNetworks,
                        importedNetworks,
                        constants.providerNetworks(),
                        constants.importedNetworks());
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        initEntityModelCellTables(constants, templates, resources);
        providersEditor.setLabel(constants.networkProvider());
        providersEditor.addWrapperStyleName(style.providersStyle());
        driver.initialize(this);
    }

    Iterable<ExternalNetwork> getAllImportedNetworks() {
        ListModel<ExternalNetwork> tableModel = importedNetworks.asEditor().flush();
        return tableModel != null && tableModel.getItems() != null ? tableModel.getItems()
                : new ArrayList<ExternalNetwork>();
    }

    public void refreshImportedNetworks() {
        importedNetworks.asEditor().edit(importedNetworks.asEditor().flush());
    }

    void initEntityModelCellTables(final ApplicationConstants constants,
            final ApplicationTemplates templates,
            final ApplicationResources resources) {

        providerNetworks.addColumn(new TextColumnWithTooltip<ExternalNetwork>() {
            @Override
            public String getValue(ExternalNetwork model) {
                return model.getDisplayName();
            }
        }, constants.nameNetworkHeader());

        importedNetworks.addColumn(new EditTextColumnWithTooltip<ExternalNetwork>(new FieldUpdater<ExternalNetwork, String>() {
            @Override
            public void update(int index, ExternalNetwork model, String value) {
                model.setDisplayName(value);
            }
        }) {
            @Override
            public String getValue(ExternalNetwork model) {
                return model.getDisplayName();
            }
        }, constants.nameNetworkHeader());

        Column<ExternalNetwork, String> idColumn = new TextColumnWithTooltip<ExternalNetwork>() {
            @Override
            public String getValue(ExternalNetwork model) {
                return model.getNetwork().getProvidedBy().getExternalId();
            }
        };
        providerNetworks.addColumn(idColumn, constants.idNetworkHeader());
        importedNetworks.addColumn(idColumn, constants.idNetworkHeader());

        dcColumn = new ListModelListBoxColumn<ExternalNetwork, StoragePool>(new NullSafeRenderer<StoragePool>() {
            @Override
            public String renderNullSafe(StoragePool dc) {
                return dc.getName();
            }
        })
        {
            @Override
            public ListModel<StoragePool> getValue(ExternalNetwork network) {
                return network.getDataCenters();
            }
        };
        importedNetworks.addColumn(dcColumn, constants.dcNetworkHeader());

        CheckboxHeader publicAllHeader =
                new CheckboxHeader(templates.textForCheckBoxHeader(constants.publicNetwork())) {
                    @Override
                    protected void selectionChanged(Boolean value) {
                        for (ExternalNetwork model : getAllImportedNetworks()) {
                            model.setPublicUse(value);
                        }
                        refreshImportedNetworks();
                    }

                    @Override
                    public Boolean getValue() {
                        for (ExternalNetwork model : getAllImportedNetworks()) {
                            if (!model.isPublicUse()) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean isEnabled() {
                        return true;
                    }

                    @Override
                    public void render(Context context, SafeHtmlBuilder sb) {
                        super.render(context, sb);
                        sb.append(templates.inlineImageWithTitle(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.dialogIconHelp())
                                .getHTML()),
                                constants.networkPublicUseLabel()));
                    }
                };

        importedNetworks.addColumn(new CheckboxColumn<ExternalNetwork>(new FieldUpdater<ExternalNetwork, Boolean>() {
            @Override
            public void update(int index, ExternalNetwork model, Boolean value) {
                model.setPublicUse(value);
                refreshImportedNetworks();
            }
        }) {
            @Override
            public Boolean getValue(ExternalNetwork model) {
                return model.isPublicUse();
            }

            @Override
            protected boolean canEdit(ExternalNetwork model) {
                return true;
            }

            @Override
            public void render(Context context, ExternalNetwork object, SafeHtmlBuilder sb) {
                super.render(context, object, sb);
                sb.append(templates.textForCheckBox("")); //$NON-NLS-1$
            }

        }, publicAllHeader, "80px"); //$NON-NLS-1$
    }

    @Override
    public void edit(ImportNetworksModel model) {
        splitTable.edit(model.getProviderNetworks(),
                model.getImportedNetworks(),
                model.getAddImportCommand(),
                model.getCancelImportCommand());
        driver.edit(model);
    }

    @Override
    public ImportNetworksModel flush() {
        return driver.flush();
    }

    @Override
    public void validateImportedNetworks(List<String> errors) {
        importedNetworks.validate(errors);
    }

}
