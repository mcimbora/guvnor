/*
 * Copyright 2011 JBoss Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.drools.guvnor.client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import org.drools.guvnor.client.common.FormStylePopup;
import org.drools.guvnor.client.explorer.ClientFactory;
import org.drools.guvnor.client.explorer.ExplorerViewCenterPanel;
import org.drools.guvnor.client.explorer.TabManager;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.resources.Images;
import org.drools.guvnor.client.rpc.PushClient;
import org.drools.guvnor.client.rpc.PushResponse;
import org.drools.guvnor.client.rpc.ServerPushNotification;
import org.drools.guvnor.client.ruleeditor.MultiViewEditor;
import org.drools.guvnor.client.ruleeditor.MultiViewRow;
import org.drools.guvnor.client.widgets.tables.AssetPagedTable;

import java.util.Arrays;
import java.util.List;

public class TabOpenerImpl
        implements
        TabManager {

    private Constants constants = GWT.create( Constants.class );
    private static Images images = GWT.create( Images.class );

    private final ExplorerViewCenterPanel explorerViewCenterPanel;
    private final ClientFactory clientFactory;

    protected TabOpenerImpl(ClientFactory clientFactory,
                            ExplorerViewCenterPanel explorerViewCenterPanel) {
        this.clientFactory = clientFactory;
        this.explorerViewCenterPanel = explorerViewCenterPanel;
    }

    public void openAssetsToMultiView(MultiViewRow[] rows) {

        String blockingAssetName = null;
        final String[] uuids = new String[rows.length];
        final String[] names = new String[rows.length];

        for (int i = 0; i < rows.length; i++) {
            // Check if any of these assets are already opened.
            if ( explorerViewCenterPanel.showIfOpen( rows[i].uuid ) ) {
                blockingAssetName = rows[i].name;
                break;
            }
            uuids[i] = rows[i].uuid;
            names[i] = rows[i].name;
        }

        if ( blockingAssetName != null ) {
            FormStylePopup popup = new FormStylePopup( images.information(),
                    constants.Asset0IsAlreadyOpenPleaseCloseItBeforeOpeningMultiview( blockingAssetName ) );
            popup.show();
            return;
        }

        MultiViewEditor multiview = new MultiViewEditor( rows,
                clientFactory );

        multiview.setCloseCommand( new Command() {
            public void execute() {
                explorerViewCenterPanel.close( Arrays.toString( uuids ) );
            }
        } );

        explorerViewCenterPanel.addTab( Arrays.toString( names ),
                multiview,
                uuids.toString() );

    }


    public void openPackageViewAssets(final String packageUuid,
                                      final String packageName,
                                      String key,
                                      final List<String> formatInList,
                                      Boolean formatIsRegistered,
                                      final String itemName) {
        if ( !explorerViewCenterPanel.showIfOpen( key ) ) {

            String feedUrl = GWT.getModuleBaseURL()
                    + "feed/package?name="
                    + packageName
                    + "&viewUrl="
                    + Util.getSelfURL()
                    + "&status=*";
            final AssetPagedTable table = new AssetPagedTable(
                    packageUuid,
                    formatInList,
                    formatIsRegistered,
                    feedUrl,
                    clientFactory );
            explorerViewCenterPanel.addTab( itemName
                    + " ["
                    + packageName
                    + "]",
                    table,
                    key );

            final ServerPushNotification sub = new ServerPushNotification() {
                public void messageReceived(PushResponse response) {
                    if ( response.messageType.equals( "packageChange" )
                            && response.message.equals( packageName ) ) {
                        table.refresh();
                    }
                }
            };
            PushClient.instance().subscribe( sub );
            table.addUnloadListener( new Command() {
                public void execute() {
                    PushClient.instance().unsubscribe( sub );
                }
            } );
        }
    }

    public boolean showIfOpen(String id) {
        return explorerViewCenterPanel.showIfOpen( id );
    }
}
