/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ibm.ws.massive.resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.massive.LoginInfo;
import com.ibm.ws.massive.LoginInfoEntry;
import com.ibm.ws.massive.RepositoryBackendException;
import com.ibm.ws.massive.RepositoryBackendIOException;
import com.ibm.ws.massive.RepositoryBackendRequestFailureException;
import com.ibm.ws.massive.RepositoryException;
import com.ibm.ws.massive.internal.AbstractMassive;
import com.ibm.ws.massive.sa.client.BadVersionException;
import com.ibm.ws.massive.sa.client.MassiveClient;
import com.ibm.ws.massive.sa.client.RequestFailureException;
import com.ibm.ws.massive.sa.client.model.AppliesToFilterInfo;
import com.ibm.ws.massive.sa.client.model.Asset;
import com.ibm.ws.massive.sa.client.model.Attachment;
import com.ibm.ws.massive.sa.client.model.AttachmentSummary;
import com.ibm.ws.massive.sa.client.model.FilterVersion;
import com.ibm.ws.massive.sa.client.model.StateAction.Action;
import com.ibm.ws.massive.sa.client.model.WlpInformation;

/**
 * This class is used as the base class to provide an API for java applications to interface with
 * assets stored within massive. The data structure used to store assets in massive is kept separate
 * from the representation of a resource to protect calling applications from underlying changes to
 * massive or the data structure used.
 *
 * If this is class is used to read data from Massive, bear in mind that the asset information obtained
 * from massive is cached when first read. If you need to ensure that the data contained is up to date
 * please call the "refreshFromMassive" method which will reload the asset from massive.
 */
public abstract class MassiveResource extends AbstractMassive {

    // The backing asset for this resource
    protected Asset _asset;

    // The appliesTo header helper
    protected AppliesToHeaderHelper _appliesToHelper;

    /**
     * Massive connection information
     */
    protected static final String MASSIVE_ASSET_URL = "http://marketplace01.usca.ibm.com:9002/ma/v1/assets/";
    protected MassiveClient _client;

    /**
     * A flag to indicate whether the 'content' attachment has been attached.
     * Currently we only allow one such attachment.
     */
    private boolean _contentAttached = false;

    /**
     * IMPORTANT: Do not use this field directly, instead use getAttachments. This
     * field is updated lazily, it is not populated when the resource is created, but
     * instead upon the first call to getAttachments
     * Use a hashmap so we can use the string equals when checking if this resource already
     * has an attachment with that name
     */
    private HashMap<String, AttachmentResource> _attachments = new HashMap<String, AttachmentResource>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The enums in this class map to enums in the com.ibm.ws.massive.sa.client.model package. There
     * are the enums that are exposed as API whereas the code in com.ibm.ws.massive.sa.client.model is
     * the structure we use in massive and is hidden from code using this API.
     */

    /**
     * License Enum
     *
     * This is an Enum of known license types. See
     * http://www-03.ibm.com/software/sla/sladb.nsf/viewbla/
     */
    public enum LicenseType {
        IPLA(Asset.LicenseType.IPLA), // International Program License Agreement - applies to warranted IBM programs
        ILAN(Asset.LicenseType.ILAN), // International License Agreement for Non-Warranted Programs
        ILAE(Asset.LicenseType.ILAE), // International License Agreement for Evaluation of Programs
        ILAR(Asset.LicenseType.ILAR), // International License Agreement for Early Release of Programs
        UNSPECIFIED(Asset.LicenseType.UNSPECIFIED); // Samples are uploaded with an UNSPECIFIED license type

        Asset.LicenseType _license;

        private LicenseType(Asset.LicenseType l) {
            _license = l;
        }

    }

    // Key in assetInfo.properties under which to store the type of the license
    public static final String LICENSE_TYPE = "licenseType";

    /**
     * ------------------------------------------------------------------------------------------------
     * Visibility Enum
     * ------------------------------------------------------------------------------------------------
     */
    public enum Visibility {
        PUBLIC(WlpInformation.Visibility.PUBLIC),
        PRIVATE(WlpInformation.Visibility.PRIVATE),
        PROTECTED(WlpInformation.Visibility.PROTECTED),
        INSTALL(WlpInformation.Visibility.INSTALL);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final WlpInformation.Visibility _vis;
        private static Map<WlpInformation.Visibility, Visibility> _lookup;

        /**
         * This method is used to obtain the MassiveResource.Visibility given the WlpInformation.Visibility value
         *
         * @param v The WlpInformation.Visibility type which is contained within the asset
         * @return The MassiveResource.Visibility enum value which maps to the supplied WlpInformation.Visibility
         *         enum value
         */
        static Visibility lookup(WlpInformation.Visibility v) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<WlpInformation.Visibility, Visibility>();
                    for (Visibility vis : Visibility.values()) {
                        _lookup.put(vis.getWlpVisibility(), vis);
                    }
                }
            }
            return _lookup.get(v);
        }

        private Visibility(WlpInformation.Visibility v) {
            _vis = v;
        }

        WlpInformation.Visibility getWlpVisibility() {
            return _vis;
        }
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * DownloadPolicy Enum
     * ------------------------------------------------------------------------------------------------
     */
    public enum DownloadPolicy {
        INSTALLER(WlpInformation.DownloadPolicy.INSTALLER),
        ALL(WlpInformation.DownloadPolicy.ALL);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final WlpInformation.DownloadPolicy _policy;
        private static Map<WlpInformation.DownloadPolicy, DownloadPolicy> _lookup;

        /**
         * This method is used to obtain the MassiveResource.DownloadPolicy given the WlpInformation.DownloadPolicy value
         *
         * @param v The WlpInformation.DownloadPolicy type which is contained within the asset
         * @return The MassiveResource.DownloadPolicy enum value which maps to the supplied WlpInformation.DownloadPolicy
         *         enum value
         */
        static DownloadPolicy lookup(WlpInformation.DownloadPolicy p) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<WlpInformation.DownloadPolicy, DownloadPolicy>();
                    for (DownloadPolicy policy : DownloadPolicy.values()) {
                        _lookup.put(policy.getWlpDownloadPolicy(), policy);
                    }
                }
            }
            return _lookup.get(p);
        }

        private DownloadPolicy(WlpInformation.DownloadPolicy p) {
            _policy = p;
        }

        WlpInformation.DownloadPolicy getWlpDownloadPolicy() {
            return _policy;
        }
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * DownloadPolicy Enum
     * ------------------------------------------------------------------------------------------------
     */
    public enum DisplayPolicy {
        HIDDEN(WlpInformation.DisplayPolicy.HIDDEN),
        VISIBLE(WlpInformation.DisplayPolicy.VISIBLE);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final WlpInformation.DisplayPolicy _policy;
        private static Map<WlpInformation.DisplayPolicy, DisplayPolicy> _lookup;

        /**
         * This method is used to obtain the MassiveResource.DownloadPolicy given the WlpInformation.DownloadPolicy value
         *
         * @param v The WlpInformation.DownloadPolicy type which is contained within the asset
         * @return The MassiveResource.DownloadPolicy enum value which maps to the supplied WlpInformation.DownloadPolicy
         *         enum value
         */
        static DisplayPolicy lookup(WlpInformation.DisplayPolicy p) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<WlpInformation.DisplayPolicy, DisplayPolicy>();
                    for (DisplayPolicy policy : DisplayPolicy.values()) {
                        _lookup.put(policy.getWlpDisplayPolicy(), policy);
                    }
                }
            }
            return _lookup.get(p);
        }

        private DisplayPolicy(WlpInformation.DisplayPolicy p) {
            _policy = p;
        }

        WlpInformation.DisplayPolicy getWlpDisplayPolicy() {
            return _policy;
        }
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * State Enum
     * ------------------------------------------------------------------------------------------------
     */
    /**
     * This interface is implemented by the enum. It is change the state of the asset.
     */
    private interface PerformStateAction {
        public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException;
    }

    /**
     * Local action enum, used in {@link RepositoryResourceLifecycleException} to convey the action being performed
     * on an asset, when the exception occurred.
     */
    public enum StateAction implements PerformStateAction {
        /**
         * Move the state from {@link State#draft} to {@link State#awaiting_approval}
         */
        PUBLISH {
            @Override
            public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException {
                resource.publish();
            }
        },
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#published}
         */
        APPROVE {
            @Override
            public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException {
                resource.approve();
            }
        },
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#draft}
         */
        CANCEL {
            @Override
            public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException {
                resource.cancel();
            }
        },
        /**
         * Move the state from {@link State#awaiting_approval} to {@link State#need_more_info}
         */
        NEED_MORE_INFO {
            @Override
            public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException {
                resource.need_more_info();
            }
        },
        /**
         * Move the state from {@link State#published} to {@link State#draft}
         */
        UNPUBLISH {
            @Override
            public void performAction(MassiveResource resource) throws RepositoryBackendException, RepositoryResourceException {
                resource.unpublish();
            }
        }
    }

    /**
     * This interface is implemented by the enum. It is change the state of the asset.
     */
    private interface StateMachine {
        public void publish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException;

        public void approve(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException;

        public void cancel(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException;

        public void need_more_info(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException;

        public void unpublish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException;

        public StateAction getNextActionForMovingToState(State target);
    }

    /**
     * This enum represents the state of an asset, and the allowable transitions for the same.
     */
    public enum State implements StateMachine {
        /**
         * The asset is in draft state, valid transitions are to publish.
         */
        DRAFT(Asset.State.DRAFT) {
            @Override
            public void publish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.PUBLISH);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on publish", ioe);
                } catch (RequestFailureException cause) {
                    throw new RepositoryBackendRequestFailureException(cause);
                }
            }

            @Override
            public void approve(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Approve not supported for assets in draft state",
                                ass.get_id(),
                                DRAFT,
                                StateAction.APPROVE);
            }

            @Override
            public void cancel(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Cancel not supported for assets in draft state",
                                ass.get_id(),
                                DRAFT,
                                StateAction.CANCEL);
            }

            @Override
            public void need_more_info(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Need_nore_info not supported for assets in draft state",
                                ass.get_id(),
                                DRAFT,
                                StateAction.NEED_MORE_INFO);
            }

            @Override
            public void unpublish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Unpublish not supported for assets in draft state",
                                ass.get_id(),
                                DRAFT,
                                StateAction.NEED_MORE_INFO);
            }

            @Override
            public StateAction getNextActionForMovingToState(State target) {
                switch (target) {
                    case AWAITING_APPROVAL:
                        return StateAction.PUBLISH;
                    case DRAFT:
                        return null;
                    case NEED_MORE_INFO:
                        return StateAction.PUBLISH;
                    case PUBLISHED:
                        return StateAction.PUBLISH;
                    default:
                        // We should have covered all the cases but return null if a new state is created that
                        // we are currently unaware of
                        return null;
                }
            }
        },

        /**
         * The asset is in awaiting_approval valid transitions are to approve, cancel, or need_more_info
         */
        AWAITING_APPROVAL(Asset.State.AWAITING_APPROVAL) {
            @Override
            public void publish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Publish not supported for assets in awaiting_approval state",
                                ass.get_id(),
                                AWAITING_APPROVAL,
                                StateAction.PUBLISH);
            }

            @Override
            public void approve(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.APPROVE);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on approve", ioe);
                } catch (RequestFailureException cause) {
                    throw new RepositoryBackendRequestFailureException(cause);
                }
            }

            @Override
            public void cancel(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.CANCEL);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on cancel", ioe);
                } catch (RequestFailureException bfe) {
                    throw new RepositoryBackendRequestFailureException(bfe);
                }
            }

            @Override
            public void need_more_info(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.NEED_MORE_INFO);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on need_more_info", ioe);
                } catch (RequestFailureException bfe) {
                    throw new RepositoryBackendRequestFailureException(bfe);
                }
            }

            @Override
            public void unpublish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Unpublish not supported for assets in awaiting_approval state",
                                ass.get_id(),
                                AWAITING_APPROVAL,
                                StateAction.UNPUBLISH);
            }

            @Override
            public StateAction getNextActionForMovingToState(State target) {
                switch (target) {
                    case AWAITING_APPROVAL:
                        return null;
                    case DRAFT:
                        return StateAction.CANCEL;
                    case NEED_MORE_INFO:
                        return StateAction.NEED_MORE_INFO;
                    case PUBLISHED:
                        return StateAction.APPROVE;
                    default:
                        // We should have covered all the cases but return null if a new state is created that
                        // we are currently unaware of
                        return null;
                }
            }
        },

        /**
         * The asset is in need_more_info state, valid transitions are to publish
         */
        NEED_MORE_INFO(Asset.State.NEED_MORE_INFO) {
            @Override
            public void publish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.PUBLISH);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on need_more_info", ioe);
                } catch (RequestFailureException bfe) {
                    throw new RepositoryBackendRequestFailureException(bfe);
                }
            }

            @Override
            public void approve(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Approve not supported for assets in need _more_info state",
                                ass.get_id(),
                                NEED_MORE_INFO,
                                StateAction.APPROVE);
            }

            @Override
            public void cancel(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Cancel not supported for assets in need _more_info state",
                                ass.get_id(),
                                NEED_MORE_INFO,
                                StateAction.CANCEL);
            }

            @Override
            public void need_more_info(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Need_more_info not supported for assets in need _more_info state",
                                ass.get_id(),
                                NEED_MORE_INFO,
                                StateAction.NEED_MORE_INFO);
            }

            @Override
            public void unpublish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Unpublish not supported for assets in need_more_info state",
                                ass.get_id(),
                                NEED_MORE_INFO,
                                StateAction.UNPUBLISH);
            }

            @Override
            public StateAction getNextActionForMovingToState(State target) {
                switch (target) {
                    case NEED_MORE_INFO:
                        return null;
                    default:
                        return StateAction.PUBLISH;
                }
            }
        },

        /**
         * The asset is in published state, there are no valid transitions from this state.
         */
        PUBLISHED(Asset.State.PUBLISHED) {
            @Override
            public void publish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Publish not supported for assets in published state",
                                ass.get_id(),
                                PUBLISHED,
                                StateAction.PUBLISH);
            }

            @Override
            public void approve(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Approve not supported for assets in published state",
                                ass.get_id(),
                                PUBLISHED,
                                StateAction.APPROVE);
            }

            @Override
            public void cancel(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Cancel not supported for assets in published state",
                                ass.get_id(),
                                PUBLISHED,
                                StateAction.CANCEL);
            }

            @Override
            public void need_more_info(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException {
                throw new RepositoryResourceLifecycleException("Need_more_info not supported for assets in published state",
                                ass.get_id(),
                                PUBLISHED,
                                StateAction.NEED_MORE_INFO);
            }

            @Override
            public void unpublish(MassiveClient cli, Asset ass) throws RepositoryResourceLifecycleException, RepositoryBackendException {
                try {
                    cli.updateState(ass.get_id(), Action.UNPUBLISH);
                } catch (IOException ioe) {
                    throw new RepositoryBackendIOException("IOException on need_more_info", ioe);
                } catch (RequestFailureException bfe) {
                    throw new RepositoryBackendRequestFailureException(bfe);
                }
            }

            @Override
            public StateAction getNextActionForMovingToState(State target) {
                switch (target) {
                    case PUBLISHED:
                        return null;
                    default:
                        return StateAction.UNPUBLISH;
                }
            }
        };

        private final Asset.State _state;
        private static Map<Asset.State, State> _lookup;

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        /**
         * This method is used to obtain the MassiveResource.State given the Asset.State value
         *
         * @param s The Asset.State type which is contained within the asset
         * @return The MassiveResource.State enum value which maps to the supplied Asset.State enum value
         */
        static State lookup(Asset.State s) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<Asset.State, State>();
                    for (State state : State.values()) {
                        _lookup.put(state.getAssetState(), state);
                    }
                }
            }
            return _lookup.get(s);
        }

        private State(Asset.State s) {
            _state = s;
        }

        Asset.State getAssetState() {
            return _state;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Type Enum
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * This interface is implemented by the enum. It is used to create an instance of a resource
     * based on the type of asset.
     */
    private interface ResourceFactory {
        public <T extends MassiveResource> T createResource(LoginInfoEntry loginInfoResource);
    }

    /**
     * The type of resource.
     */
    @SuppressWarnings("unchecked")
    public enum Type implements ResourceFactory {
        /**
         * Samples that only use product code
         */
        PRODUCTSAMPLE(Asset.Type.PRODUCTSAMPLE, TypeLabel.PRODUCTSAMPLE, "samples") {
            @Override
            public SampleResource createResource(LoginInfoEntry loginInfoResource) {
                SampleResource sr = new SampleResource(loginInfoResource);

                // Sample resources represents two different types make sure the right one is set
                sr.setType(PRODUCTSAMPLE);
                return sr;
            }
        },
        /**
         * Samples that also use open source code
         */
        OPENSOURCE(Asset.Type.OPENSOURCE, TypeLabel.OPENSOURCE, "osi") {
            @Override
            public SampleResource createResource(LoginInfoEntry loginInfoResource) {
                SampleResource sr = new SampleResource(loginInfoResource);

                // Sample resources represents two different types make sure the right one is set
                sr.setType(OPENSOURCE);
                return sr;
            }
        },
        /**
         * A liberty install jar file
         */
        INSTALL(Asset.Type.INSTALL, TypeLabel.INSTALL, "runtimes") {
            @Override
            public ProductResource createResource(LoginInfoEntry loginInfoResource) {
                ProductResource pr = new ProductResource(loginInfoResource);

                // Product resources represents two different types make sure the right one is set
                pr.setType(INSTALL);
                return pr;
            }
        },
        /**
         * Extended functionality for the install
         */
        ADDON(Asset.Type.ADDON, TypeLabel.ADDON, "addons") {
            @Override
            public ProductResource createResource(LoginInfoEntry loginInfoResource) {
                ProductResource pr = new ProductResource(loginInfoResource);

                // Product resources represents two different types make sure the right one is set
                pr.setType(ADDON);
                return pr;
            }
        },
        /**
         * ESA's representing features
         */
        FEATURE(Asset.Type.FEATURE, TypeLabel.FEATURE, "features") {
            @Override
            public EsaResource createResource(LoginInfoEntry loginInfoResource) {
                return new EsaResource(loginInfoResource);
            }
        },
        /**
         * Ifixes
         */
        IFIX(Asset.Type.IFIX, TypeLabel.IFIX, "ifxes") {
            @Override
            public IfixResource createResource(LoginInfoEntry loginInfoResource) {
                return new IfixResource(loginInfoResource);
            }
        },
        /**
         * AdminScripts
         */
        ADMINSCRIPT(Asset.Type.ADMINSCRIPT, TypeLabel.ADMINSCRIPT, "scripts") {
            @Override
            public AdminScriptResource createResource(LoginInfoEntry loginInfoResource) {
                return new AdminScriptResource(loginInfoResource);
            }
        },
        /**
         * Config snippets
         */
        CONFIGSNIPPET(Asset.Type.CONFIGSNIPPET, TypeLabel.CONFIGSNIPPET, "snippets") {
            @Override
            public ConfigSnippetResource createResource(LoginInfoEntry loginInfoResource) {
                return new ConfigSnippetResource(loginInfoResource);
            }
        },
        /**
         * Tools
         */
        TOOL(Asset.Type.TOOL, TypeLabel.TOOL, "tools") {
            @Override
            public ToolResource createResource(LoginInfoEntry loginInfoResource) {
                return new ToolResource(loginInfoResource);
            }
        };

        private final Asset.Type _type;
        private final TypeLabel _typeLabel;
        private final String _typeForURL;
        private static Map<Asset.Type, Type> _lookup;

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        /**
         * This method is used to obtain the MassiveResource.Type given the WlpInformation.Type value
         *
         * @param t The WlpInformation.Type type which is contained within the asset
         * @return The MassiveResource.Type enum value which maps to the supplied WlpInformation.Type enum value
         */
        static Type lookup(Asset.Type t) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<Asset.Type, Type>();
                    for (Type type : Type.values()) {
                        _lookup.put(type.getAssetType(), type);
                    }
                }
            }
            return _lookup.get(t);
        }

        private Type(Asset.Type t, TypeLabel label, String typeForUrl) {
            _type = t;
            _typeLabel = label;
            _typeForURL = typeForUrl;
        }

        public TypeLabel getTypeLabel() {
            return _typeLabel;
        }

        Asset.Type getAssetType() {
            return _type;
        }

        public String getURLForType() {
            return _typeForURL;
        }

        // get the long name of a the type ie for FEATURE returns "com.ibm.websphere.Feature"
        public String getValue() {
            return getAssetType().getValue();
        }
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * TypeLabel Enum
     * ------------------------------------------------------------------------------------------------
     */
    public enum TypeLabel {

        PRODUCTSAMPLE(WlpInformation.TypeLabel.PRODUCTSAMPLE),
        OPENSOURCE(WlpInformation.TypeLabel.OPENSOURCE),
        INSTALL(WlpInformation.TypeLabel.INSTALL),
        ADDON(WlpInformation.TypeLabel.ADDON),
        FEATURE(WlpInformation.TypeLabel.FEATURE),
        IFIX(WlpInformation.TypeLabel.IFIX),
        ADMINSCRIPT(WlpInformation.TypeLabel.ADMINSCRIPT),
        CONFIGSNIPPET(WlpInformation.TypeLabel.CONFIGSNIPPET),
        TOOL(WlpInformation.TypeLabel.TOOL);

        private final WlpInformation.TypeLabel _typeLabel;
        private static Map<WlpInformation.TypeLabel, TypeLabel> _lookup = null;

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        /**
         * This method is used to obtain the MassiveResource.TypeLabel given the WlpInformation.TypeLabel value
         *
         * @param t The WlpInformation.TypeLabel type which is contained within the asset
         * @return The MassiveResource.TypeLabel enum value which maps to the supplied WlpInformation.TypeLabel enum value
         */
        static TypeLabel lookup(WlpInformation.TypeLabel label) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<WlpInformation.TypeLabel, TypeLabel>();
                    for (TypeLabel l : TypeLabel.values()) {
                        _lookup.put(l.getWlpTypeLabel(), l);
                    }
                }
            }
            return _lookup.get(label);
        }

        private TypeLabel(WlpInformation.TypeLabel typeLabel) {
            _typeLabel = typeLabel;
        }

        WlpInformation.TypeLabel getWlpTypeLabel() {
            return _typeLabel;
        }

        @Override
        public String toString() {
            return _typeLabel.getValue();
        }
    }

    /**
     * A match result enum used for checking if a resource matches a product definition.
     * Values are <br>
     * <I>MATCHED</I> : The resource matches<br>
     * <I>NOT_APPLICABLE</I> : The resource doesn't apply to the specified product or the resource
     * has no applies to information.<br>
     * <I>INVALID_VERSION</I>: The resource does not apply to the specified version<br>
     * <I>INVALID_EDITION</I>: The resource does not apply to the specified edition<br>
     * <I>INVALID_INSTALL_TYPE</I>: The resource does not apply to the specified install type.
     */
    public enum MatchResult {
        MATCHED, NOT_APPLICABLE, INVALID_VERSION, INVALID_EDITION, INVALID_INSTALL_TYPE;
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Constructor Code
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * The first logInfoResource from the collection is used. Deprecated in favor of using the constructor that takes
     * one LoginInfoResource.
     *
     * @param loginInfo
     */
//    @Deprecated
//    public MassiveResource(LoginInfo loginInfo) {
//        this(loginInfo.get(0));
//    }

    /**
     * Constructor, requires logon information to massive
     *
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     */
    public MassiveResource(LoginInfoEntry loginInfoResource) {
        super(loginInfoResource);
        _asset = new Asset();
        _asset.setWlpInformation(new WlpInformation());
        _client = new MassiveClient(loginInfoResource == null ? null : loginInfoResource.getClientLoginInfo());

        // By default all assets are downloadable
        setDownloadPolicy(DownloadPolicy.ALL);
    }

    /**
     * Creates a new resource using the same logon infomation as this resource
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T extends MassiveResource> T createNewResource() {

        T result;
        if (null == getType()) {
            result = (T) createTestResource(_loginInfoResource);
        } else {
            Type t = MassiveResource.Type.lookup(_asset.getType());
            result = t.createResource(_loginInfoResource);
        }
        return result;
    }

    // TODO make this private and invoke using reflection TASK 119220 raised
    private static MassiveResource createTestResource(LoginInfoEntry loginInfoResource) {
        return new MassiveResource(loginInfoResource) {};
    }

    /**
     * By default this class uses a cached version of the asset which may have
     * stale data (including attachments), calling this method forces the class to grab a
     * new version of the asset from massive.
     *
     * @throws RepositoryBackendException
     */
    public void refreshFromMassive() throws RepositoryBackendException, RepositoryResourceException {
        try {
            _asset = _client.getAsset(_asset.get_id());
            parseAttachmentsInAsset();
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Unable to obtain asset from massive " + _asset.get_id(), ioe);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version in asset ", _asset.get_id(), bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe);
        }
    }

    /**
     * Check if this resources matches the supplied product definition
     *
     * @param def The product definition to check against
     * @return A {@link MatchResult} that says whether the resource matches the product definition, and if
     *         not the reason why the match failed.
     */
    public MatchResult matches(ProductDefinition def) {
        Collection<AppliesToFilterInfo> atfiList = _asset.getWlpInformation().getAppliesToFilterInfo();
        if (atfiList == null || atfiList.isEmpty()) {
            return MatchResult.NOT_APPLICABLE;
        }
        MatchResult matchResult = MatchResult.MATCHED;
        for (AppliesToFilterInfo atfi : atfiList) {
            if (!!!atfi.getProductId().equals(def.getId())) {
                // This one isn't applicable, maybe the next one is
                matchResult = MatchResult.NOT_APPLICABLE;
                continue;
            } else {
                if (def.getVersion() != null && !def.getVersion().isEmpty()) {

                    Version checkVersion = new Version(def.getVersion());
                    VersionRange vr = FilterVersion.getFilterRange(atfi.getMinVersion(), atfi.getMaxVersion());

                    if (!vr.includes(checkVersion)) {
                        return MatchResult.INVALID_VERSION;
                    }
                }

                if (atfi.getRawEditions() != null && !!!atfi.getRawEditions().isEmpty() && !!!atfi.getRawEditions().contains(def.getEdition())) {
                    return MatchResult.INVALID_EDITION;
                }

                if (atfi.getInstallType() != null && !!!atfi.getInstallType().equals(def.getInstallType())) {
                    return MatchResult.INVALID_INSTALL_TYPE;
                }

                // Got here so this must have been a match, only need one of the array to match, not all
                return MatchResult.MATCHED;
            }
        }
        return matchResult;
    }

    /**
     * This method reads the attachments from massive, effectively syncing up with massive.
     *
     * @throws RepositoryBackendException
     */
    protected synchronized void parseAttachmentsInAsset() throws RepositoryBackendException {
        readAttachmentsFromAsset(_asset);
    }

    /**
     * Read the attachments from the supplied asset and create an AttachmentResource to represent them
     * and then store them in our AttachmentResource list
     *
     * @param ass
     * @throws RepositoryBackenAttachment
     */
    private synchronized void readAttachmentsFromAsset(Asset ass) throws RepositoryBackendException {
        Collection<Attachment> attachments = ass.getAttachments();

        _attachments = new HashMap<String, AttachmentResource>();
        if (attachments != null) {
            for (Attachment at : attachments) {
                _attachments.put(at.getName(), new AttachmentResource(at));

                if (AttachmentType.lookup(at.getType()) == AttachmentType.CONTENT) {
                    _contentAttached = true;
                }
            }
        }

    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Standard Asset Fields
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Assigns the asset associated with this resource
     *
     * @param ass The asset to use
     */
    protected void setAsset(Asset ass) {
        _asset = ass;
    }

    /**
     * Gets the asset associated with this resource
     *
     * @return the asset associated with this resource
     */
    protected Asset getAsset() {
        return _asset;
    }

    public void setLoginInfoEntry(LoginInfoEntry loginInfoResource) {
        _loginInfoResource = loginInfoResource;
        _client = new MassiveClient(loginInfoResource == null ? null : loginInfoResource.getClientLoginInfo());
        // Using a new repo - we should blow away the id.
        resetId();
    }

    public LoginInfoEntry getLoginInfo() {
        return _loginInfoResource;
    }

    public String get_id() {
        return _asset.get_id();
    }

    public void resetId() {
        _asset.set_id(null);
    }

    public void setName(String name) {
        _asset.setName(name);
    }

    public String getName() {
        return _asset.getName();
    }

    public void setProvider(Provider provider) {
        _asset.setProvider(provider == null ? null : provider.getProvider());
    }

    public Provider getProvider() {
        return new Provider(_asset.getProvider());
    }

    protected void setType(Type type) {
        if (type == null) {
            _asset.setType(null);
            _asset.getWlpInformation().setTypeLabel(null);
        } else {
            _asset.setType(type.getAssetType());
            _asset.getWlpInformation().setTypeLabel(type.getTypeLabel().getWlpTypeLabel());
        }
    }

    public Type getType() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return Type.lookup(_asset.getType());
    }

    public TypeLabel getTypeLabel() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return TypeLabel.lookup(_asset.getWlpInformation().getTypeLabel());
    }

    public void setDescription(String desc) {
        _asset.setDescription(desc);
    }

    public String getDescription() {
        return _asset.getDescription();
    }

    public State getState() {
        return State.lookup(_asset.getState());
    }

    public void setState(State state) {
        _asset.setState(state == null ? null : state.getAssetState());
    }

    public void setVersion(String version) {
        _asset.setVersion(version);
    }

    public String getVersion() {
        return _asset.getVersion();
    }

    public void setAppliesToHeaderHelper(AppliesToHeaderHelper a) {
        this._appliesToHelper = a;
    }

    public void setMainAttachmentSize(long size) {
        _asset.getWlpInformation().setMainAttachmentSize(size);
    }

    public long getMainAttachmentSize() {
        return _asset.getWlpInformation().getMainAttachmentSize();
    }

    public void setDownloadPolicy(DownloadPolicy policy) {
        _asset.getWlpInformation().setDownloadPolicy(policy == null ? null : policy.getWlpDownloadPolicy());
    }

    public DownloadPolicy getDownloadPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return DownloadPolicy.lookup(_asset.getWlpInformation().getDownloadPolicy());
    }

    /**
     * Sets the relative URL that should be used to display this resource
     *
     * @return The URL used to display this resource
     */
    public String getVanityURL() {
        return _asset.getWlpInformation().getVanityRelativeURL();
    }

    /**
     * Sets the featured weight. This is a String value although it will be a number from 1 to 9, with 9 being the highest weight.
     * Not having a value set means it is lower in priority than any that have a weight.
     *
     * @param featuredWeight
     */
    public void setFeaturedWeight(String featuredWeight) {
        _asset.getWlpInformation().setFeaturedWeight(featuredWeight);
    }

    /**
     * Gets the featured weight. This is a String value although it will be a number from 1 to 9, with 9 being the highest weight.
     * Not having a value set means it is lower in priority than any that have a weight.
     *
     * @return the featuredWeight
     */
    public String getFeaturedWeight() {
        return _asset.getWlpInformation().getFeaturedWeight();
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Attachment Methods
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Adds the supplied attachment to the asset as the main, or "content" attachment.
     * There can be only one attachment of AttachmentType.CONTENT.
     * The filename is used as the name of the attachment.
     *
     * TODO: addContent looks like the mirror of getMainAttachment, but the logic behind
     * getMainAttachment is all about file extension, and not AttachmentType.
     *
     * @param file The attachment to be added
     * @param update Set to true if the attachment is to be updated, false if it is
     *            a new attachment to be added
     * @return Returns the AttachmentResource representation of the supplied file
     */
    public AttachmentResource addContent(File file) throws RepositoryException {
        return addContent(file, file.getName());
    }

    /**
     * Add content from a file with a name specified
     *
     * @param file
     * @param name
     * @return
     * @throws RepositoryException
     */
    public AttachmentResource addContent(File file, String name) throws RepositoryException {
        if (_contentAttached) {
            throw new RepositoryResourceValidationException("addContent(" + file.getAbsolutePath()
                                                            + ") called for resource " + getName() + " which all ready has a CONTENT attachment",
                            get_id());
        }
        _contentAttached = true;
        AttachmentResource at = addAttachment(file, AttachmentType.CONTENT, name);
        setMainAttachmentSize(at.getSize());
        return at;
    }

    /**
     * Add content based on a content file, name and URL for an external file
     *
     * @param file
     * @param name
     * @param url
     * @return
     * @throws RepositoryException
     */
    public AttachmentResource addContent(File file, String name, String url, AttachmentLinkType linkType) throws RepositoryException {
        if (_contentAttached) {
            throw new RepositoryResourceValidationException("addContent(" + file.getAbsolutePath()
                                                            + ") called for resource " + getName() + " which all ready has a CONTENT attachment",
                            get_id());
        }
        _contentAttached = true;
        AttachmentResource at = addAttachment(file, AttachmentType.CONTENT, name, url, linkType);
        setMainAttachmentSize(at.getSize());
        return at;
    }

    public AttachmentResource addAttachment(File file, AttachmentType type) throws RepositoryException {
        return addAttachment(file, type, file.getName());
    }

    /**
     * Adds the supplied attachment to the asset under the supplied name
     *
     * @param file The attachment to be added
     * @param name The name to use for the attachment
     *            a new attachment to be added
     * @return Returns the AttachmentResource representation of the supplied file
     * @throws RepositoryException
     */
    public synchronized AttachmentResource addAttachment(File file, AttachmentType type, String name) throws RepositoryException {
        AttachmentResource at = new AttachmentResource(file, name);
        at.setType(type);
        // Massive can set this, but we need to know it before then, as we may want to compare this attachment
        // size with the size of an attachment already uploaded.
        at.setFileProps();
        _attachments.put(name, at);
        return at;
    }

    public synchronized AttachmentResource addAttachment(File file, AttachmentType type, String name, String url) throws RepositoryException {
        AttachmentResource at = addAttachment(file, type, name, url, AttachmentLinkType.DIRECT);
        return at;
    }

    public synchronized AttachmentResource addAttachment(File file, AttachmentType type, String name, String url, AttachmentLinkType linkType) throws RepositoryException {
        AttachmentResource at = new AttachmentResource(file, name, url, linkType);
        at.setType(type);
        // Since we are storing the attachment outside of massive we have to work out the size ourselves
        at.setFileProps();
        _attachments.put(name, at);
        return at;
    }

    /**
     * This method checks the supplied attachment to see if its the "main" one. The default behaviour is to
     * check if the attachment is a registered as type=content, if it is then it assumes it is the "main" attachment.
     *
     * @param at The attachment to check
     * @return True if this is considered the "main" attachment for the asset
     */
    boolean isMainAttachment(AttachmentResource at) {
        return (AttachmentType.CONTENT == at.getType());
    }

    /**
     * Gets the "main" attachment for this resource. It will cycle through the assets asking the
     * resource if the attachment is the "main" one, it will stop once it finds the first
     * attachment that satisfies that criteria and returns an {@link OldAttachment} TODO: Should we throw an exception if we have more than one suitable main attachment?
     * We may have to support multiple main payloads if an asset has multiple different payloads
     * for different license types, but in that case maybe we should provide a
     * "getMainAttachment (String licenseType)" method - which would also only return one attachment
     * and go bang if more than one is found.
     *
     * @return
     * @throws RepositoryBackendException
     */
    public AttachmentResource getMainAttachment() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();

        for (AttachmentResource at : attachments) {
            if (isMainAttachment(at)) {
                return at;
            }
        }

        return null;
    }

    /**
     * Get the attachment id for the specified attachment
     *
     * @param attachmentName The name of the attachment whose id is to be returned
     * @return The id of the supplied attachment
     * @throws RepositoryBackendException
     */
    public String getAttachmentId(String attachmentName) throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();

        for (AttachmentResource at : attachments) {
            if (at.getName().equals(attachmentName)) {
                return at.get_id();
            }
        }
        return null;
    }

    /**
     * Get an {@link AttachmentResource} for the specified attachment name
     *
     * @param attachmentName The name of the attachment to look for
     * @return An {@link AttachmentResource} object that matches the supplied name
     * @throws RepositoryBackendException
     */
    public AttachmentResource getAttachment(String attachmentName) throws RepositoryBackendException, RepositoryResourceException {
        for (AttachmentResource at : getAttachments()) {
            if (at.getName().equals(attachmentName)) {
                return at;
            }
        }
        return null;
    }

    /**
     * Get an {@link AttachmentResource} for the specified attachment id
     *
     * @param attachmentName The name of the attachment to look for
     * @return An {@link AttachmentResource} object that matches the supplied id
     * @throws RepositoryBackendException
     */
    public AttachmentResource getAttachmentFromId(String id) throws RepositoryBackendException, RepositoryResourceException {
        for (AttachmentResource at : getAttachments()) {
            if (at.get_id().equals(id)) {
                return at;
            }
        }
        return null;
    }

    /**
     * This returns a a {@link Collection} of {@link AttachmentResource} objects associated with the asset. We cache
     * the list of attachments by obtaining a new asset if the current asset doesn't have any. To force
     * the asset to be refreshed from massive call {@link #refreshFromMassive()} method.
     *
     * @return a {@link List} of {@link AttachmentResource} objects. This collection is unmodifiable, however
     *         methods like delete may be called on the AttachmentResources within the collection
     * @throws RepositoryException
     */
    public synchronized Collection<AttachmentResource> getAttachments() throws RepositoryBackendException, RepositoryResourceException {
        if (_attachments == null || _attachments.isEmpty()) {
            // Read the resource back from massive, we don't call refresh from massive as it will
            // read all the resource info back and we just want the attachments
            // Might just be an asset that hasn't been uploaded yet and has no attachments
            if (get_id() != null) {
                MassiveResource mr = MassiveResource.getResource(getLoginInfo(), get_id());
                readAttachmentsFromAsset(mr._asset);
            } else {
                if (_attachments == null) {
                    _attachments = new HashMap<String, MassiveResource.AttachmentResource>();
                }
            }
        }
        return Collections.unmodifiableCollection(_attachments.values());
    }

    /**
     * Gets the number of attachments associated with this resource
     *
     * @return The number of attachments associated with this resource
     * @throws RepositoryBackendException
     */
    public int getAttachmentCount() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();
        return attachments.size();
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * LICENSE METHODS
     * ------------------------------------------------------------------------------------------------
     */

    public void addLicense(File license, Locale loc) throws RepositoryException {
        AttachmentResource res = addAttachment(license, AttachmentType.LICENSE, license.getName());
        res.setLocale(loc);
    }

    public void addLicenseAgreement(File license, Locale loc) throws RepositoryException {
        AttachmentResource res = addAttachment(license, AttachmentType.LICENSE_AGREEMENT, license.getName());
        res.setLocale(loc);
    }

    public void addLicenseInformation(File license, Locale loc) throws RepositoryException {
        AttachmentResource res = addAttachment(license, AttachmentType.LICENSE_INFORMATION, license.getName());
        res.setLocale(loc);
    }

    public AttachmentResource getLicense(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        AttachmentSummary s = matchByLocale(getAttachments(), Attachment.Type.LICENSE, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    public AttachmentResource getLicenseAgreement(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        // Get the attachment resource which has Type.LICENSE and Locale loc
        AttachmentSummary s = matchByLocale(getAttachments(), Attachment.Type.LICENSE_AGREEMENT, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    public AttachmentResource getLicenseInformation(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        // Get the attachment resource which has Type.LICENSE and Locale loc
        AttachmentSummary s = matchByLocale(getAttachments(), Attachment.Type.LICENSE_INFORMATION, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    /*
     * Find an AttachmentResource in a given set which either has the desired Locale
     * or failing that, the same language as the desired locale.
     */
    static AttachmentSummary matchByLocale(Collection<? extends AttachmentSummary> attachments,
                                           Attachment.Type desiredType, Locale desiredLocale) {
        Collection<AttachmentSummary> possibleMatches = new ArrayList<AttachmentSummary>();
        for (AttachmentSummary s : attachments) {
            Attachment.Type attType = s.getAttachment().getType();

            if (attType.equals(desiredType)) {
                // This is either an exact match, or a candidate for a match-by-language
                if (s.getLocale().equals(desiredLocale)) {
                    return s;
                } else {
                    possibleMatches.add(s);
                }
            }
        }

        Locale baseDesiredLocale = new Locale(desiredLocale.getLanguage());
        for (AttachmentSummary s : possibleMatches) {
            if (s.getLocale().equals(baseDesiredLocale)) {
                return s;
            }
        }

        // If we've found no better match, can we at least provide English?
        for (AttachmentSummary s : possibleMatches) {
            if (s.getLocale().equals(Locale.ENGLISH)) {
                return s;
            }
        }

        // Getting desperate: do we have anything that's language is English?
        for (AttachmentSummary s : possibleMatches) {
            if (new Locale(s.getLocale().getLanguage()).equals(Locale.ENGLISH)) {
                return s;
            }
        }

        return null;
    }

    public void setLicenseType(LicenseType lt) {
        if (lt == null) {
            return;
        }
        Asset.LicenseType internalLicenseType = Asset.LicenseType.valueOf(lt.toString());
        _asset.setLicenseType(internalLicenseType);
    }

    public LicenseType getLicenseType() {
        Asset.LicenseType lt = _asset.getLicenseType();

        return (lt == null ? null : MassiveResource.LicenseType.valueOf(lt.toString()));
    }

    /**
     * Set the LicenseId.
     * In features, this is the value of the Subsystem-License header.
     * In products, it's currently null (31st Jan 2014)
     * It's undefined for all other asset types.
     */
    public void setLicenseId(String lic) {
        _asset.setLicenseId(lic);
    }

    /**
     * Gets the value of the LicenseId.
     * FRor features, this is contained in the Subsystem-License header
     */
    public String getLicenseId() {
        return _asset.getLicenseId();
    }

    /**
     * Sets the short description of the asset
     *
     * @param shortDescription The short description to use for the asset
     */
    public void setShortDescription(String shortDescription) {
        _asset.setShortDescription(shortDescription);
    }

    /**
     * Get the short description of the asset
     *
     * @return The short description of the asset
     */
    public String getShortDescription() {
        return _asset.getShortDescription();
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * HELPER METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Decide whether an attachment needs updating.
     *
     * @return boolean - whether the attachment needs updating
     */
    public UpdateType updateRequired(MassiveResource matching) {

        if (null == matching) {
            // No matching asset found
            return UpdateType.ADD;
        }

        if (equivalentWithoutAttachments(matching)) {
            return UpdateType.NOTHING;
        } else {
            // As we are doing an update set our id to be the one we found in massive
            // Not needed now, we are merging both assets
            _asset.set_id(matching.get_id());
            return UpdateType.UPDATE;
        }
    }

    /**
     * This creates an object which can be used to compare with another resource's to determine if
     * they represent the same asset.
     *
     * @return
     */
    public MassiveResourceMatchingData createMatchingData() {
        MassiveResourceMatchingData matchingData = new MassiveResourceMatchingData();
        matchingData.setName(getName());
        matchingData.setProvider(getProvider().getName());
        matchingData.setType(getType());
        return matchingData;
    }

    protected List<MassiveResource> performMatching() throws IOException, BadVersionException, RequestFailureException, RepositoryBadDataException, RepositoryBackendException {
        List<MassiveResource> matching = new ArrayList<MassiveResource>();

        // connect to massive and find that sample
        Collection<MassiveResource> resources = getAllResourcesWithDupes(getType(), new LoginInfo(_loginInfoResource));

        MassiveResource resource = null;
        for (MassiveResource res : resources) {
            if (createMatchingData().equals(res.createMatchingData())) {
                // found an asset on massive - get the full asset.
                resource = getResource(_loginInfoResource, res.get_id());
                matching.add(resource);
            }
        }

        return matching;
    }

    /**
     * This method tries to find out if there is a match for "this" resource already in massive.
     *
     * @return A list of resources that were found in massive which has the same name, provider and type as this
     *         resource, an empty list otherwise.
     * @throws RepositoryResourceValidationException
     * @throws RepositoryBackendException
     */
    public List<MassiveResource> findMatchingResource() throws RepositoryResourceValidationException, RepositoryBackendException, RepositoryBadDataException {
        List<MassiveResource> matchingRes;
        try {
            if (null == getProvider()) {
                throw new RepositoryResourceValidationException("No provider specified for the supplied resource", this.get_id());
            }
            matchingRes = performMatching();
            if (matchingRes != null && matchingRes.size() > 1) {
                StringBuilder warningMessage = new StringBuilder("More than one match found for " + getName() + ":");
                for (MassiveResource massiveResource : matchingRes) {
                    warningMessage.append("\n\t" + massiveResource.getName() + " (" + massiveResource.get_id() + ")");
                }
                logger.warning(warningMessage.toString());
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Exception thrown when attempting to find a matching asset to " +
                                                   get_id(), ioe);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("BadDataException accessing asset", get_id(), bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe);
        }
        return matchingRes;
    }

    /**
     * Resources should override this method to copy fields that should be used as part of an
     * update
     *
     * @param fromResource
     */
    protected void copyFieldsFrom(MassiveResource fromResource, boolean includeAttachmentInfo) {
        setName(fromResource.getName()); // part of the identification so locked
        setDescription(fromResource.getDescription());
        setShortDescription(fromResource.getShortDescription());
        setProvider(fromResource.getProvider()); // part of the identification so locked
        setVersion(fromResource.getVersion());
        setDownloadPolicy(fromResource.getDownloadPolicy());
        setLicenseId(fromResource.getLicenseId());
        setLicenseType(fromResource.getLicenseType());
        setMainAttachmentSize(fromResource.getMainAttachmentSize());
        setFeaturedWeight(fromResource.getFeaturedWeight());

        if (includeAttachmentInfo) {
            setMainAttachmentSize(fromResource.getMainAttachmentSize());
        }
        _asset.getWlpInformation().setAppliesToFilterInfo(fromResource.getAsset().getWlpInformation().getAppliesToFilterInfo());
    }

    /**
     * This method copies the fields from "this" that we care about to the "fromResource". Then we
     * set our asset to point to the one in "fromResource". In effect this means we get all the details
     * from the "fromResource" and override fields we care about and store the merged result in our asset.
     *
     * This is used when we have read an asset back from massive, the asset read back from massive will have more
     * fields that in it (that massive has set) so this method is used to copy the asset containing those extra
     * fields from the "from" resource to the our resource. Note that the from resource is modified during this
     * process, so do not rely on it's contents not changing. This is only used to copy the contents from a matching
     * resource into a new resource and then the matching resource object is discarded so this is currently safe.
     * TODO: Find a better way of doing this
     *
     * @param fromResource
     * @throws RepositoryResourceValidationException
     */
    /* package */void overWriteAssetData(MassiveResource fromResource, boolean includeAttachmentInfo) throws RepositoryResourceValidationException {

        // Make sure we are dealing with the same type....this
        // should never happen
        if (!fromResource.getClass().getName().equals(getClass().getName())) {
            throw new RepositoryResourceValidationException("Expected class of type " + getClass().getName()
                                                            + " but was " + fromResource.getClass().getName(), this.get_id());
        }
        // copy the stuff into target
        fromResource.copyFieldsFrom(this, includeAttachmentInfo);

        // Now use target
        _asset = fromResource._asset;
    }

    /* package */void addAsset() throws RepositoryResourceCreationException, RepositoryBadDataException {
        // ensure the resource does not have an id - if we read a resource back from massive, change it, then reupload it
        // then we need to remove the id as massive won't allow us to push an asset into massive with an id
        resetId();
        try {
            _asset = _client.addAsset(_asset);
        } catch (IOException ioe) {
            throw new RepositoryResourceCreationException("Failed to add the asset", get_id(), ioe);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when adding asset", get_id(), bvx);
        } catch (RequestFailureException rfe) {
            throw new RepositoryResourceCreationException("Failed to add the asset", get_id(), rfe);
        } catch (SecurityException se) {
            throw new RepositoryResourceCreationException("Failed to add the asset", get_id(), se);
        }
    }

    /* package */void updateAsset() throws RepositoryResourceUpdateException,
                    RepositoryResourceValidationException, RepositoryBadDataException {
        try {
            _asset = _client.updateAsset(_asset);
        } catch (IOException ioe) {
            throw new RepositoryResourceUpdateException("Failed to update the asset", this.get_id(), ioe);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when updating asset", this.get_id(), bvx);
        } catch (RequestFailureException e) {
            throw new RepositoryResourceUpdateException("Failed to update the attachment", this.get_id(), e);
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to update the asset", this.get_id(), se);
        }
    }

    /**
     * Sets our asset object to point to the asset inside the "from" resource
     *
     * @param from
     */
    /* package */void copyAsset(MassiveResource from) {
        _asset = from._asset;
    }

    /* package */void addAttachment(AttachmentResource at) throws RepositoryResourceCreationException,
                    RepositoryBadDataException, RepositoryResourceUpdateException {
        // ensure the attachment does not have an id - if we read a resource back from massive, change it, then re-upload it
        // then we need to remove the id as massive won't allow us to push an asset into massive with an id
        at.resetId();
        try {
            _client.addAttachment(get_id(), at);
        } catch (IOException e) {
            throw new RepositoryResourceCreationException("Failed to add the attachment", this.get_id(), e);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when adding attachment", this.get_id(), bvx);
        } catch (RequestFailureException rfe) {
            throw new RepositoryResourceCreationException("Failed to add the attachment", this.get_id(), rfe);
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to add the attachment", this.get_id(), se);
        }
    }

    /* package */void updateAttachment(AttachmentResource at) throws RepositoryResourceUpdateException,
                    RepositoryBadDataException {
        try {
            _client.updateAttachment(get_id(), at);
        } catch (IOException e) {
            throw new RepositoryResourceUpdateException("Failed to update the attachment", this.get_id(), e);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when updating attachment", this.get_id(), bvx);
        } catch (RequestFailureException e) {
            throw new RepositoryResourceUpdateException("Failed to update the attachment", this.get_id(), e);
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to update the attachment", this.get_id(), se);
        }

    }

    /**
     * Uploads the resource to the repository using the supplied strategy
     *
     * @param strategy
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public synchronized void uploadToMassive(UploadStrategy strategy) throws RepositoryBackendException, RepositoryResourceException {
        updateGeneratedFields();
        // If the asset has already been uploaded to massive (i.e. it has an id) then read and copy any
        // attachment stored in massive
        if (get_id() != null) {
            copyAttachments();
        }
        List<MassiveResource> matching = strategy.findMatchingResources(this);
        strategy.uploadAsset(this, matching);
    }

    /**
     * Does a deep copy of attachments that are stored in the massive backend. Downloads the files to a temp
     * dir (and deletes them on JVM exit), sets the URL to null and associates the downloaded file with the
     * resource. This will cause massive to store the attachment and create a new URL for it.
     *
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    //@SuppressWarnings("rawtypes")
    void copyAttachments() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();
        for (AttachmentResource at : attachments) {
            AttachmentLinkType linkType = at.getLinkType();
            if ((null == linkType) && (at.getURL() != null)) {
                final File tempFile = new File(get_id() + "_" + at.getName());

                try {
                    // Why do we have to specify a return type for the run method and paramatize
                    // PrivilegedExceptionAction to it, this method should have a void return type ideally.
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() {
                            tempFile.deleteOnExit();
                            return null;
                        }
                    });
                } catch (PrivilegedActionException e) {
                    throw new RepositoryResourceValidationException("Unable to copy attachments", at.get_id(), e.getCause());
                }

                try {
                    try {
                        at.downloadToFile(tempFile);
                    } catch (IOException e) {
                        // try once more, if we fail then give up on this asset
                        at.downloadToFile(tempFile);
                    }
                    at.setURL(null);
                    at.setFile(tempFile);
                } catch (IOException e) {
                    // tried twice, give up :(
                    throw new RepositoryResourceUpdateException("Exception caught while obtaining attachments for resource " + getName(),
                                    get_id(), e);
                }
            }

            // Reset ID after pulling down (if needed) the attachment
            at.resetId();
        }
    }

    public void publish() throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        s.publish(_client, _asset);
        refreshFromMassive();
    }

    public void approve() throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        s.approve(_client, _asset);
        refreshFromMassive();
    }

    public void cancel() throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        s.cancel(_client, _asset);
        refreshFromMassive();
    }

    public void need_more_info() throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        s.need_more_info(_client, _asset);
        refreshFromMassive();
    }

    public void unpublish() throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        s.unpublish(_client, _asset);
        refreshFromMassive();
    }

    /**
     * Moves the resource to the desired state
     *
     * @param resource
     * @param state
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void moveToState(State state) throws RepositoryBackendException, RepositoryResourceException {
        int counter = 0;
        while (getState() != state) {
            counter++;
            StateAction nextAction = getState().getNextActionForMovingToState(state);
            if (nextAction != null) {
                nextAction.performAction(this);
            }

            if (counter >= 10) {
                throw new RepositoryResourceLifecycleException("Unable to move to state " + state +
                                                               " after 10 state transistion attempts. Resource left in state " + getState(),
                                get_id(), getState(), nextAction);
            }
        }
    }

    /**
     * Deletes the asset and all the attachments
     * TODO: Should we make this invalid now and blow up if anyone tries to alter the values in
     * this resource? Or just blow up if anyone tries to flow any data back to massive? Keep it alive
     * for "reading" purposes only?
     *
     * @throws RepositoryResourceDeletionException
     */
    public void delete() throws RepositoryResourceDeletionException {
        try {
            _client.deleteAssetAndAttachments(_asset.get_id());
        } catch (IOException ioe) {
            throw new RepositoryResourceDeletionException("Failed to delete resource", this.get_id(), ioe);
        } catch (RequestFailureException e) {
            throw new RepositoryResourceDeletionException("Failed to delete resource", this.get_id(), e);
        }
    }

    /**
     * Returns a URL which represent the URL that can be used to view the asset in Massive. This is more
     * for testing purposes, the assets can be access programatically via various methods on this class.
     *
     * @param asset - an Asset
     * @return String - the asset URL
     */
    public String getAssetURL() {
        String url = _loginInfoResource.getRepositoryUrl() + "/assets/" + _asset.get_id() + "?";
        if (_loginInfoResource.getUserId() != null) {
            url += "userId=" + _loginInfoResource.getUserId();
        }
        if (_loginInfoResource.getUserId() != null && _loginInfoResource.getPassword() != null) {
            url += "&password=" + _loginInfoResource.getPassword();
        }
        if (_loginInfoResource.getApiKey() != null) {
            url += "&apiKey=" + _loginInfoResource.getApiKey();
        }
        return url;
    }

    /**
     * (Re)calculate any fields that may be based on information within this resource.
     */
    protected void updateGeneratedFields() {
        //update the asset filter info.
        updateAssetFilterInfo();
        createVanityURL();
    }

    /**
     * Returns empty string by defualt (no version string used)
     *
     * @return
     */
    protected String getVersionForVanityUrl() {
        return "";
    }

    protected String getNameForVanityUrl() {
        return getName();
    }

    protected void createVanityURL() {
        List<Character> allowable = Arrays.asList(
                                                  'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                                                  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                                                  '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                                                  '.', '-', '_');
        Type type = getType();
        if (type == null) {
            return;
        }
        StringBuffer sb = new StringBuffer(type.getURLForType());
        String version = getVersionForVanityUrl();
        if (version != null && !version.isEmpty()) {
            sb.append("-");
            sb.append(version);
        }
        sb.append("-");
        sb.append(getNameForVanityUrl());

        // Now filter out any non white listed character and replace spaces with underscores
        StringBuffer filtered = new StringBuffer();
        for (Character c : sb.toString().toCharArray()) {
            if (allowable.contains(c)) {
                filtered.append(c);
            } else if (c.equals(' ')) {
                filtered.append('_');
            }
        }
        _asset.getWlpInformation().setVanityRelativeURL(filtered.toString());
    }

    private void updateAssetFilterInfo() {
        WlpInformation wlp = _asset.getWlpInformation();
        if (wlp != null) {
            String appliesTo = wlp.getAppliesTo();
            if (appliesTo != null) {
                List<AppliesToFilterInfo> atfi = AppliesToProcessor.parseAppliesToHeader(appliesTo, _appliesToHelper);
                if (atfi != null) {
                    wlp.setAppliesToFilterInfo(atfi);
                }
            }
        }
    }

    /**
     * This method dumps a formatted JSON string to the supplied out stream
     *
     * @param os The output stream the JSON should be dumped too.
     */
    public void dump(OutputStream os) {
        updateGeneratedFields();
        _asset.dump(os);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_asset == null) ? 0 : _asset.hashCode());
        return result;
    }

    /**
     * Checks if the two resources are equivalent by checking if the assets
     * are equivalent.
     *
     * @param obj
     * @return
     */
    public boolean equivalent(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MassiveResource other = (MassiveResource) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equivalent(other._asset))
            return false;
        return true;
    }

    /**
     * Checks if the two resources are equivalent by checking if the assets
     * are equivalent.
     *
     * @param obj
     * @return
     */
    public boolean equivalentWithoutAttachments(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MassiveResource other = (MassiveResource) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equivalentWithoutAttachments(other._asset))
            return false;
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MassiveResource other = (MassiveResource) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equals(other._asset))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "<MassiveResource@" + this.hashCode() + " <Asset=" + _asset + ">>";
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * STATIC HELPER METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * This method gets all the resources in Massive
     *
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return A {@link Collection} of {@link MassiveResource} object
     * @throws RepositoryBackendException
     */
    public static Collection<MassiveResource> getAllResources(LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<MassiveResource> resources = new ResourceList<MassiveResource>();
        for (LoginInfoEntry loginInfoResource : loginInfo) {
            MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
            List<Asset> assets;
            try {
                assets = client.getAllAssets();
                for (Asset ass : assets) {
                    resources.add(createResourceFromAsset(ass, loginInfoResource));
                }
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
        }
        return resources;
    }

    /**
     * This method gets all the resources in Massive
     *
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return A {@link Collection} of {@link MassiveResource} object
     * @throws RepositoryBackendException
     */
    protected static Collection<MassiveResource> getAllResourcesWithDupes(LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<MassiveResource> resources = new ArrayList<MassiveResource>();
        for (LoginInfoEntry loginInfoResource : loginInfo) {
            MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
            List<Asset> assets;
            try {
                assets = client.getAllAssets();
                for (Asset ass : assets) {
                    resources.add(createResourceFromAsset(ass, loginInfoResource));
                }
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
        }
        return resources;
    }

    /**
     * Gets all resources of the specified type from Massive
     *
     * @param type The {@link MassiveResource.Type} of resource to obtain
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return A {@link Collection} of {@link MassiveResource} object
     * @throws RepositoryBackendException
     */
    protected static <T extends MassiveResource> Collection<T> getAllResourcesWithDupes(Type type, LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<T> resources = new ArrayList<T>();
        for (LoginInfoEntry loginInfoResource : loginInfo) {
            MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
            Collection<Asset> assets;
            try {
                assets = client.getAssets(type.getAssetType());
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
            for (Asset ass : assets) {
                T res = createResourceFromAsset(ass, loginInfoResource);
                resources.add(res);
            }
        }
        return resources;
    }

    /**
     * Gets all resources of the specified type from Massive
     *
     * @param type The {@link MassiveResource.Type} of resource to obtain
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return A {@link Collection} of {@link MassiveResource} object
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public static <T extends MassiveResource> Collection<T> getAllResources(Type type, LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<T> resources = new ResourceList();
        for (LoginInfoEntry loginInfoResource : loginInfo) {
            MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
            Collection<Asset> assets;
            try {
                assets = client.getAssets(type.getAssetType());
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
            for (Asset ass : assets) {
                T res = createResourceFromAsset(ass, loginInfoResource);
                resources.add(res);
            }
        }
        return resources;
    }

    /**
     * Gets all resources of the specified <code>types</code> from Massive returning only those that are relevant to the <code>productDefinition</code>.
     *
     * @param productDefinition The product that these resources will be installed into. Can be <code>null</code> or empty indicating resources for any product should be obtained
     *            (they will just be filtered by type).
     * @param types The {@link MassiveResource.Type} of resource to obtain. <code>null</code> indicates that all types should be obtained.
     * @param visibility The {@link Visibility} of resources to obtain. <code>null</code> indicates that resources with any visibility should be obtained. This is only relevant if
     *            {@link Type#FEATURE} is one of the <code>types</code> being obtained, it is ignored for other types.
     * @param loginInfo the login information for the repository. Must not be <code>null</code>.
     * @return A Map mapping the type to the {@link Collection} of {@link MassiveResource} object. There may be a <code>null</code> collection for a supplied type, this indicates
     *         no assets of that type were found
     * @throws RepositoryBackendException
     */
    public static Map<Type, Collection<? extends MassiveResource>> getResources(Collection<ProductDefinition> productDefinitions, Collection<Type> types, Visibility visibility,
                                                                                LoginInfo loginInfo) throws RepositoryBackendException {
        /*
         * We've been asked to get all the resources that apply to a set of products. Some of the values we can filter on on the server and some we can't. Those we can filter on
         * are:
         * -- Product ID
         * -- Version (see comment below)
         * -- Visibility (see other comment below)
         * 
         * The fields we can't filter on are:
         * -- Edition: no edition = all editions and you can't search for a missing field
         * -- InstallType: not install type = all install types and you can't search for a missing field
         * 
         * Version is special as there are two ways we version content in the repository. It may have a specific version (minVersion=maxVersion) or a just a specific version (no
         * maxVersion). As we can't search for a missing field there is an additional field stored on the applies to filter info to allow us to search for the absence of a max
         * version. All massive queries use AND so we first search for the specific version then for the absence of the max version.
         * 
         * Visibility is also special as it only applies to features. If you are getting anything other than features and put the visibility on the URL then it will come back with
         * zero hits (or only hits for features) so we can only do this efficiently in Massive if only searching for features, otherwise have to do it here.
         * 
         * Once we have all the results back then feed it into the matches method to a) ensure that the content that just has a min version is in the right range b) also filter out
         * anything from fields that we can't filter on the server.
         */
        com.ibm.ws.massive.sa.client.model.WlpInformation.Visibility visibilityForMassiveFilter = null;
        if (visibility != null) {
            if (types != null && types.size() == 1 && types.contains(Type.FEATURE)) {
                // As per comment above, can only use the visibility on the URL filter if we are only looking for features
                visibilityForMassiveFilter = visibility.getWlpVisibility();
            }
        }

        Collection<com.ibm.ws.massive.sa.client.model.Asset.Type> assetTypes = null;
        if (types != null && !types.isEmpty()) {
            assetTypes = new HashSet<Asset.Type>();
            for (Type type : types) {
                assetTypes.add(type.getAssetType());
            }
        }

        Collection<String> productIds = new HashSet<String>();
        Collection<String> productVersions = new HashSet<String>();
        if (productDefinitions != null) {
            for (ProductDefinition productDefinition : productDefinitions) {
                String id = productDefinition.getId();
                if (id != null) {
                    productIds.add(id);
                }
                String version = productDefinition.getVersion();
                if (version != null) {
                    productVersions.add(version);
                }
            }
        }
        try {
            Collection<MassiveResource> resources = new ResourceList<MassiveResource>();
            for (LoginInfoEntry loginInfoEntry : loginInfo) {
                MassiveClient client = new MassiveClient(loginInfoEntry.getClientLoginInfo());
                // We may end up with duplicate assets from these two calls but that is ok as we are using the ResourceList as the collection of resources which removes duplicates
                Collection<Asset> assets = client.getAssets(assetTypes, productIds, visibilityForMassiveFilter, productVersions);
                assets.addAll(client.getAssetsWithUnboundedMaxVersion(assetTypes, productIds, visibilityForMassiveFilter));
                for (Asset asset : assets) {
                    resources.add(createResourceFromAsset(asset, loginInfoEntry));
                }
            }

            // This will have returned some assets that aren't valid due to version or stuff we can't filter on so run it through a local filtering as well
            Map<Type, Collection<? extends MassiveResource>> returnMap = new HashMap<MassiveResource.Type, Collection<? extends MassiveResource>>();
            for (MassiveResource massiveResource : resources) {
                Type type = massiveResource.getType();
                if (Type.FEATURE == type && visibility != null) {
                    EsaResource esa = (EsaResource) massiveResource;
                    Visibility visibilityMatches = esa.getVisibility() == null ? Visibility.PUBLIC : esa.getVisibility();
                    if (!visibilityMatches.equals(visibility)) {
                        // Visibility is different, ignore this feature
                        continue;
                    }
                }

                // If there is no product definitions defined then say it matches - not being filtered to a specific version
                boolean matches = productDefinitions == null || productDefinitions.isEmpty();
                if (productDefinitions != null) {
                    for (ProductDefinition productDefinition : productDefinitions) {
                        if (massiveResource.matches(productDefinition) == MatchResult.MATCHED) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (!matches) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Collection<MassiveResource> resourcesOfType = (Collection<MassiveResource>) returnMap.get(type);
                if (resourcesOfType == null) {
                    resourcesOfType = new HashSet<MassiveResource>();
                    returnMap.put(type, resourcesOfType);
                }
                resourcesOfType.add(massiveResource);
            }
            return returnMap;
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
        } catch (RequestFailureException e) {
            throw new RepositoryBackendRequestFailureException(e);
        }
    }

    /**
     * Gets all resources of the specified type and license type from Massive
     *
     * @param licenseType The {@link MassiveResource.LicenseType} of the resources to obtain
     * @param type The {@link MassiveResource.Type} of resource to obtain
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return A {@link Collection} of {@link MassiveResource} object
     * @throws RepositoryBackendException
     */
    public static <T extends MassiveResource> Collection<T> getAllResources(LicenseType licenseType,
                                                                            Type type, LoginInfo loginInfo) throws RepositoryBackendException {
        Collection<T> assets = getAllResources(type, loginInfo);
        Iterator<T> it = assets.iterator();
        while (it.hasNext()) {
            T next = it.next();
            LicenseType lt = next.getLicenseType();
            if (lt == null && licenseType != null || lt != null && !lt.equals(licenseType)) {
                it.remove();
            }
        }
        return assets;
    }

    /**
     * Gets the specific resource specified by the supplied id
     *
     * @param id The id of resource to obtain
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return The resource for the specified id, returns null if no asset was found with the specified id
     * @throws RepositoryBackendException
     * @throws RepositoryBadDataException
     */
    public static <T extends MassiveResource> T getResource(LoginInfoEntry loginInfoResource, String id) throws RepositoryBackendException, RepositoryBadDataException {
        MassiveClient client = new MassiveClient(loginInfoResource.getClientLoginInfo());
        T res = null;
        try {
            Asset ass = client.getAsset(id);
            if (null != ass) {
                res = createResourceFromAsset(ass, loginInfoResource);
            }
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to create resource from Asset", ioe);
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("BadVersion reading attachment", id, bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe);
        }
        return res;
    }

    public static <T extends MassiveResource> T getResourceWithVanityRelativeURL(LoginInfo loginInfo, String vanityRelativeURL) throws RepositoryBackendException {
        @SuppressWarnings("unchecked")
        Collection<T> resources = (Collection<T>) getAllResources(loginInfo);
        for (T res : resources) {
            if (res.getVanityURL().equals(vanityRelativeURL)) {
                return res;
            }
        }
        return null;
    }

    public static Collection<MassiveResource> getAllResourcesWithVanityRelativeURL(LoginInfo loginInfo, String vanityRelativeURL) throws RepositoryBackendException {
        Collection<MassiveResource> hits = new ResourceList<MassiveResource>();

        Collection<MassiveResource> resources = getAllResources(loginInfo);
        for (MassiveResource res : resources) {
            if (res.getVanityURL().equals(vanityRelativeURL)) {
                hits.add(res);
            }
        }
        return hits;
    }

    /**
     * Creates a resources from the supplied asset
     *
     * @param ass The asset to create the resource from.
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     * @return
     * @throws RepositoryBackendException
     */

    @SuppressWarnings("unchecked")
    protected static <T extends MassiveResource> T createResourceFromAsset(Asset ass, LoginInfoEntry loginInfoResource) throws RepositoryBackendException {

        // No wlp information means no type set, so can't create a resource from this....
        T result;
        if (null == ass.getWlpInformation() ||
            ass.getType() == null) {
            result = (T) new MassiveResource(loginInfoResource) {};
        } else {
            Type t = MassiveResource.Type.lookup(ass.getType());
            result = t.createResource(loginInfoResource);
        }
        result.setAsset(ass);
        result.parseAttachmentsInAsset();
        return result;
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * PRIVATE INTERNAL METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Get the CRC of a file from an InputStream
     *
     * @param is The input stream to obtain the CRC from
     * @return a long representing the CRC value of the data read from the supplied input stream
     * @throws IOException
     */
    private static long getCRC(InputStream is) throws IOException {
        CheckedInputStream check = new CheckedInputStream(is, new CRC32());
        BufferedInputStream in = new BufferedInputStream(check);
        while (in.read() != -1) {
            // Read file in completely
        }
        long crc = check.getChecksum().getValue();
        return crc;
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * ATTACHMENT CLASSES
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * The attachment type, which can be of type THUMBNAIL, ILLUSTRATION, DOCUMENTATION, CONTENT
     */
    public enum AttachmentType {
        THUMBNAIL(Attachment.Type.THUMBNAIL),
        ILLUSTRATION(Attachment.Type.ILLUSTRATION),
        DOCUMENTATION(Attachment.Type.DOCUMENTATION),
        CONTENT(Attachment.Type.CONTENT),
        LICENSE(Attachment.Type.LICENSE), // Used for the aggregate LI+LA.html
        LICENSE_INFORMATION(Attachment.Type.LICENSE_INFORMATION),
        LICENSE_AGREEMENT(Attachment.Type.LICENSE_AGREEMENT);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final Attachment.Type _type;
        private static Map<Attachment.Type, AttachmentType> _lookup;

        /**
         * This method is used to obtain the MassiveResource.AttachmentType given the
         * Attachment.Type value
         *
         * @param t The Attachment.Type type which is contained within the asset
         * @return The MassiveResource.AttachmentType enum value which maps to the
         *         supplied Attachment.Type
         *         enum value
         */
        static AttachmentType lookup(Attachment.Type t) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<Attachment.Type, AttachmentType>();
                    for (AttachmentType type : AttachmentType.values()) {
                        _lookup.put(type.getAttachmentType(), type);
                    }
                }
            }
            return _lookup.get(t);
        }

        private AttachmentType(Attachment.Type t) {
            _type = t;
        }

        Attachment.Type getAttachmentType() {
            return _type;
        }
    }

    public enum AttachmentLinkType {
        EFD(Attachment.LinkType.EFD),
        DIRECT(Attachment.LinkType.DIRECT),
        WEB_PAGE(Attachment.LinkType.WEB_PAGE);

        // Object for the enums to lock on while updating their lookup tables
        private static Object _enumLock = new Object();

        private final Attachment.LinkType _lt;
        private static Map<Attachment.LinkType, AttachmentLinkType> _lookup;

        /**
         * This method is used to obtain the MassiveResource.Visibility given the WlpInformation.Visibility value
         *
         * @param v The WlpInformation.Visibility type which is contained within the asset
         * @return The MassiveResource.Visibility enum value which maps to the supplied WlpInformation.Visibility
         *         enum value
         */
        static AttachmentLinkType lookup(Attachment.LinkType lt) {
            synchronized (_enumLock) {
                // recheck in case someone else created before we got the lock
                if (null == _lookup) {
                    _lookup = new HashMap<Attachment.LinkType, AttachmentLinkType>();
                    for (AttachmentLinkType linkType : AttachmentLinkType.values()) {
                        _lookup.put(linkType.getWlpLinkType(), linkType);
                    }
                }
            }
            return _lookup.get(lt);
        }

        private AttachmentLinkType(Attachment.LinkType lt) {
            _lt = lt;
        }

        Attachment.LinkType getWlpLinkType() {
            return _lt;
        }
    }

    /**
     * This class provides access to details about a specific attachment
     */
    public class AttachmentResource implements AttachmentSummary {

        private final Attachment _attachment;
        private File _file = null;

        /**
         * Take a local file and store it in Massive. LinkType = null.
         *
         * @param file
         * @param name
         */
        AttachmentResource(File file, String name) {
            _attachment = new Attachment();
            _attachment.setName(name);
            _file = file;
        }

        AttachmentResource(Attachment at) {
            _attachment = at;
        }

        AttachmentResource(File file, String name, String url, AttachmentLinkType linkType) {
            _attachment = new Attachment();
            _attachment.setName(name);
            _attachment.setUrl(url);
            Attachment.LinkType wlpLinkType;
            if (linkType != null) {
                wlpLinkType = linkType.getWlpLinkType();
            } else {
                wlpLinkType = Attachment.LinkType.DIRECT;
            }
            _attachment.setLinkType(wlpLinkType);
            _file = file;
        }

        /**
         * Gets the name of the attachment (which is usually the filename of
         * the attachment, but not for features).
         *
         * @return The name of the attachment
         */
        @Override
        public String getName() {
            return _attachment.getName();
        }

        @Override
        public Locale getLocale() {
            return _attachment.getLocale();
        }

        @Override
        public File getFile() {
            return _file;
        }

        public void setFile(File file) {
            _file = file;
        }

        @Override
        public String getURL() {
            return _attachment.getUrl();
        }

        public void setURL(String url) {
            _attachment.setUrl(url);
        }

        @Override
        public Attachment getAttachment() {
            return _attachment;
        }

        /**
         * Gets the size (in bytes) of the attachment
         *
         * @return The size (in bytes) of the attachment
         */
        public long getSize() {
            return _attachment.getSize();
        }

        private void setFileProps() throws RepositoryException {
            setSize(getFileLength());
            setCRC(calculateCRC());
        }

        /**
         * Should only be set by us
         *
         * @param size
         */
        private void setSize(long size) {
            _attachment.setSize(size);
        }

        public void setImageDetails(ImageDetails details) {
            _attachment.getWlpInformation().setImageDetails(details.getImageDetails());
        }

        public ImageDetails getImageDetails() {
            return new ImageDetails(_attachment.getWlpInformation().getImageDetails());
        }

        /**
         * Gets the id of the attachment resource
         *
         * @return the id of the attachment resource
         */
        public String get_id() {
            return _attachment.get_id();
        }

        public void resetId() {
            _attachment.set_id(null);
        }

        /**
         * Gets the content type of the attachment resource
         *
         * @return the content type of the attachment resource
         */
        public String getContentType() {
            return _attachment.getContentType();
        }

        /**
         * Gets the attachment type
         *
         * @return The {@link AttachmentType attachment type} for the attachment
         */
        public AttachmentType getType() {
            return AttachmentType.lookup(_attachment.getType());
        }

        /**
         * Set the attachment type
         *
         * @param type The type of attachment
         */
        public void setType(AttachmentType type) {
            _attachment.setType(type.getAttachmentType());
        }

        /**
         * Gets the date the attachment was uploaded on
         *
         * @return The date the attachment was uploaded on
         */
        public Calendar getUploadOn() {
            return _attachment.getUploadOn();
        }

        /**
         * Gets an input stream that can be used to read the attachment contents from
         *
         * @return An input stream where the attachment can be read from
         * @throws RepositoryBackendException
         * @throws RepositoryBadDataException
         */
        public InputStream getInputStream() throws RepositoryBackendException, RepositoryBadDataException {
            try {
                return _client.getAttachment(MassiveResource.this.get_id(), get_id());
            } catch (IOException e) {
                throw new RepositoryBackendIOException("Failed to get read attachment", e);
            } catch (BadVersionException e) {
                throw new RepositoryBadDataException("BadVersion reading attachment", get_id(), e);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
        }

        public void setCRC(long CRC) {
            _attachment.getWlpInformation().setCRC(CRC);
        }

        public long getCRC() {
            return _attachment.getWlpInformation().getCRC();
        }

        public void setLocale(Locale locale) {
            _attachment.setLocale(locale);
        }

        /**
         * Deletes this attachment from massive
         *
         * @throws RepositoryResourceException
         */
        public void deleteNow() throws RepositoryResourceDeletionException {
            synchronized (MassiveResource.this) {
                try {
                    if (get_id() != null) {
                        _client.deleteAttachment(MassiveResource.this.get_id(), get_id());
                    }
                    _attachments.remove(getName());
                    if (_attachment.getType().equals(Attachment.Type.CONTENT)) {
                        _contentAttached = false;
                    }
                } catch (IOException e) {

                    throw new RepositoryResourceDeletionException("Failed to delete the attachment " + get_id() + " in asset " + MassiveResource.this.get_id(), this.get_id(), e);
                } catch (RequestFailureException e) {
                    throw new RepositoryResourceDeletionException("Failed to delete the attachment " + get_id() + " in asset " + MassiveResource.this.get_id(), this.get_id(), e);
                }
            }
        }

        /**
         * Does this attachment need updating. Is it different from the one in the
         * remote resource
         *
         * @param remoteResource
         * @return
         * @throws RepositoryBackendException
         * @throws RepositoryResourceException
         */
        public UpdateType updateRequired(MassiveResource remoteResource) throws RepositoryBackendException, RepositoryResourceException {
            if (null == remoteResource) {
                // No matching asset found
                return UpdateType.ADD;
            }

            Collection<AttachmentResource> remoteAttachments = remoteResource.getAttachments();
            for (AttachmentResource remoteAt : remoteAttachments) {
                if (getName().equals(remoteAt.getName())) {
                    if (equivalent(remoteAt)) {
                        return UpdateType.NOTHING;
                    } else {
                        return UpdateType.UPDATE;
                    }
                }
            }

            // No attachment found
            return UpdateType.ADD;
        }

        /**
         * Checks if the two resources are equivalent by checking if the attachments
         * are equivalent.
         *
         * @param obj
         * @return
         */
        public boolean equivalent(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AttachmentResource other = (AttachmentResource) obj;
            if (_attachment == null) {
                if (other._attachment != null)
                    return false;
            } else if (!_attachment.equivalent(other._attachment))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((_file == null) ? 0 : _file.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AttachmentResource other = (AttachmentResource) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (_file == null) {
                if (other._file != null)
                    return false;
            } else if (!_file.equals(other._file))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "<AttachmentResource@" + this.hashCode() + " <Attachment=" + _attachment + ">>";
        }

        /**
         * This method dumps a formatted JSON string to the supplied out stream
         *
         * @param os The output stream the JSON should be dumped too.
         */
        public void dump(OutputStream os) {
            _attachment.dump(os);
        }

        /**
         * Downloads the attachment to the specified file
         *
         * @param fileToWriteTo The file to write the attachment to.
         * @throws RepositoryBackendException
         * @throws RepositoryBadDataException
         */
        public void downloadToFile(final File fileToWriteTo) throws RepositoryBackendException,
                        IOException, RepositoryBadDataException {

            FileOutputStream fos = null;
            InputStream is = null;
            try {
                try {
                    fos = AccessController.doPrivileged(
                                    new PrivilegedExceptionAction<FileOutputStream>() {
                                        @Override
                                        public FileOutputStream run() throws FileNotFoundException {
                                            return new FileOutputStream(fileToWriteTo);
                                        }
                                    });
                } catch (PrivilegedActionException e) {
                    // Creating a FileInputStream can only return a FileNotFoundException
                    throw (FileNotFoundException) e.getCause();
                }
                is = getInputStream();

                byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            } finally {
                if (null != fos) {
                    fos.close();
                }
                if (null != is) {
                    is.close();
                }
            }
        }

        public AttachmentLinkType getLinkType() {
            Attachment.LinkType wlpLinkType = _attachment.getLinkType();
            if (wlpLinkType != null) {
                return AttachmentLinkType.lookup(wlpLinkType);
            } else {
                return null;
            }
        }

        private MassiveResource getOuterType() {
            return MassiveResource.this;
        }

        /**
         * Gets the CRC value for the this attachment.
         *
         * @return The CRC of the attachment or -1 if it wasn't found.
         * @throws RepositoryException
         * @throws RepositoryBackendException
         */
        /* package */long calculateCRC() throws RepositoryException {
            if (_file == null) {
                return 0l;
            }
            InputStream is;
            try {
                is = AccessController.doPrivileged(
                                new PrivilegedExceptionAction<FileInputStream>() {
                                    @Override
                                    public FileInputStream run() throws FileNotFoundException {
                                        return new FileInputStream(_file);
                                    }
                                });
            } catch (PrivilegedActionException e) {
                // Creating a FileInputStream can only return a FileNotFoundException or NPE
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw new RepositoryException(cause);
                } else {
                    throw (RuntimeException) cause;
                }
            }
            try {
                return MassiveResource.getCRC(is);
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }

        /**
         * Return the length of a file using a doPrivileged
         *
         * @param file
         * @return long - length of file
         * @throws RepositoryException
         */
        private long getFileLength() throws RepositoryException {
            if (_file == null) {
                return 0l;
            }
            Long fileSize;
            try {
                fileSize = AccessController.doPrivileged(new PrivilegedExceptionAction<Long>() {
                    @Override
                    public Long run() {
                        return _file.length();
                    }
                });
            } catch (PrivilegedActionException e) {
                throw new RepositoryException(e.getCause());
            }
            return fileSize.longValue();
        }
    }

    /**
     * Call the client in an unauthenticated mode to retrieve the Assets that match the supplied
     * parameters and return them as Resources.
     *
     * @param searchString
     * @param loginInfo
     * @param type
     * @param visible
     * @return A Collection of resources
     * @throws RepositoryBackendException
     */
    @SuppressWarnings("unchecked")
    public static <T extends MassiveResource> Collection<T> findResources(String searchString, LoginInfo loginInfo, TypeLabel type, Visibility visible) throws RepositoryBackendException {
        Collection<T> resources = new ResourceList();
        for (LoginInfoEntry logRes : loginInfo)
        {
            LoginInfoEntry unauthenticatedLogin = new LoginInfoEntry(null, null, logRes.getApiKey(), logRes.getRepositoryUrl(),
                            logRes.getSoftlayerUserId(), logRes.getSoftlayerPassword(),
                            logRes.getAttachmentBasicAuthUserId(), logRes.getAttachmentBasicAuthPassword());
            if (logRes.getProxy() != null) {
                unauthenticatedLogin.setProxy(logRes.getProxy());
            }

            MassiveClient unauthenticatedClient = new MassiveClient(unauthenticatedLogin.getClientLoginInfo());

            Collection<Asset> assets;
            try {
                assets = unauthenticatedClient.findAssets(searchString, type.getWlpTypeLabel(), visible.getWlpVisibility());
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("Failed to obtain the assets from massive", ioe);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e);
            }
            for (Asset ass : assets) {
                resources.add((T) createResourceFromAsset(ass, logRes));
            }
        }
        return resources;
    }
}
