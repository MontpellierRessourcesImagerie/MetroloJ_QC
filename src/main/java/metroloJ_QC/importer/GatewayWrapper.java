/*
 *  Copyright (C) 2020-2024 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package metroloJ_QC.importer;


import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ExceptionHandler;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.meta.ExperimenterWrapper;
import ome.formats.OMEROMetadataStoreClient;
import omero.api.IQueryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.AdminFacility;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ExperimenterData;
import omero.log.SimpleLogger;
import omero.model.FileAnnotationI;
import omero.model.IObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static fr.igred.omero.exception.ExceptionHandler.call;


/**
 * Basic class, contains the gateway, the security context, and multiple facilities.
 * <p>
 * Allows the user to connect to OMERO and browse through all the data accessible to the user.
 */
public abstract class GatewayWrapper {

    /** Number of requested import stores */
    private final AtomicInteger storeUses = new AtomicInteger(0);

    /** Import store lock */
    private final Lock storeLock = new ReentrantLock(true);

    /** Gateway linking the code to OMERO, only linked to one group. */
    private Gateway gateway;

    /** Security context of the user, contains the permissions of the user in this group. */
    private SecurityContext ctx;

    /** User */
    private ExperimenterWrapper user;


    /**
     * Abstract constructor of the GatewayWrapper class.
     * <p> Null arguments will be replaced with default empty objects.
     *
     * @param gateway The Gateway.
     * @param ctx     The Security Context.
     * @param user    The connected user.
     */
    protected GatewayWrapper(Gateway gateway, SecurityContext ctx, ExperimenterWrapper user) {
        this.gateway = gateway != null ? gateway : new Gateway(new SimpleLogger());
        this.user    = user != null ? user : new ExperimenterWrapper(new ExperimenterData());
        this.ctx     = ctx != null ? ctx : new SecurityContext(-1);
    }


    /**
     * Returns the Gateway.
     *
     * @return The Gateway.
     */
    public Gateway getGateway() {
        return gateway;
    }


    /**
     * Retrieves the shared import store in a thread-safe way.
     *
     * @throws DSOutOfServiceException If the connection is broken, or not logged in.
     */
    private OMEROMetadataStoreClient getImportStoreLocked()
    throws DSOutOfServiceException {
        storeLock.lock();
        try {
            return gateway.getImportStore(ctx);
        } finally {
            storeLock.unlock();
        }
    }


    /**
     * Closes the import store in a thread-safe manner.
     */
    private void closeImportStoreLocked() {
        if (storeLock.tryLock()) {
            try {
                gateway.closeImport(ctx, null);
            } finally {
                storeLock.unlock();
            }
        }
    }


    /**
     * Returns the current user.
     *
     * @return The current user.
     */
    public ExperimenterWrapper getUser() {
        return user;
    }


    /**
     * Contains the permissions of the user in the group.
     *
     * @return the {@link SecurityContext} of the user.
     */
    public SecurityContext getCtx() {
        return ctx;
    }


    /**
     * Gets the user id.
     *
     * @return The user ID.
     */
    public long getId() {
        return user.getId();
    }


    /**
     * Gets the current group ID.
     *
     * @return The group ID.
     */
    public long getCurrentGroupId() {
        return ctx.getGroupID();
    }


    /**
     * Get the ID of the current session
     *
     * @return See above
     *
     * @throws ServiceException If the connection is broken, or not logged in
     */
    public String getSessionId() throws ServiceException {
        return ExceptionHandler.of(gateway,
                                   g -> g.getSessionId(user.asDataObject()))
                               .rethrow(DSOutOfServiceException.class,
                                        ServiceException::new,
                                        "Could not retrieve session ID")
                               .get();
    }


    /**
     * Check if the client is still connected to the server
     *
     * @return See above.
     */
    public boolean isConnected() {
        return gateway.isConnected();
    }


    /**
     * Connects to OMERO using a session ID.
     *
     * @param hostname  Name of the host.
     * @param port      Port used by OMERO.
     * @param sessionId The session ID.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String sessionId)
    throws ServiceException {
        connect(new LoginCredentials(sessionId, sessionId, hostname, port));
    }


    /**
     * Connects the user to OMERO.
     * <p> Uses the argument to connect to the gateway.
     * <p> Connects to the group specified in the argument.
     *
     * @param hostname Name of the host.
     * @param port     Port used by OMERO.
     * @param username Username of the user.
     * @param password Password of the user.
     * @param groupID  ID of the group to connect.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String username, char[] password, Long groupID)
    throws ServiceException {
        LoginCredentials cred = new LoginCredentials(username,
                                                     String.valueOf(password),
                                                     hostname,
                                                     port);
        cred.setGroupID(groupID);
        connect(cred);
    }


    /**
     * Connects the user to OMERO.
     * <p> Uses the argument to connect to the gateway.
     * <p> Connects to the default group of the user.
     *
     * @param hostname Name of the host.
     * @param port     Port used by OMERO.
     * @param username Username of the user.
     * @param password Password of the user.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String username, char[] password)
    throws ServiceException {
        connect(new LoginCredentials(username,
                                     String.valueOf(password),
                                     hostname,
                                     port));
    }


    /**
     * Connects the user to OMERO. Gets the SecurityContext and the BrowseFacility.
     *
     * @param cred User credential.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(LoginCredentials cred) throws ServiceException {
        disconnect();

        try {
            this.user = new ExperimenterWrapper(gateway.connect(cred));
        } catch (DSOutOfServiceException oos) {
            throw new ServiceException(oos, oos.getConnectionStatus());
        }
        this.ctx = new SecurityContext(user.getGroupId());
        this.ctx.setExperimenter(this.user.asDataObject());
        this.ctx.setServerInformation(cred.getServer());
    }


    /**
     * Disconnects the user
     */
    public void disconnect() {
        if (isConnected()) {
            boolean sudo = ctx.isSudo();
            storeUses.set(0);
            closeImport();
            user = new ExperimenterWrapper(new ExperimenterData());
            ctx  = new SecurityContext(-1);
            ctx.setExperimenter(user.asDataObject());
            if (sudo) {
                gateway = new Gateway(gateway.getLogger());
            } else {
                gateway.disconnect();
            }
        }
    }


    /**
     * Change the current group used by the current user;
     *
     * @param groupId The group ID.
     */
    public void switchGroup(long groupId) {
        boolean sudo = ctx.isSudo();
        ctx = new SecurityContext(groupId);
        ctx.setExperimenter(user.asDataObject());
        if (sudo) {
            ctx.sudo();
        }
    }


    /**
     * Gets the {@link BrowseFacility} used to access the data from OMERO.
     *
     * @return See above.
     *
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public BrowseFacility getBrowseFacility() throws ExecutionException {
        return gateway.getFacility(BrowseFacility.class);
    }


    /**
     * Returns the {@link IQueryPrx} used to find objects on OMERO.
     *
     * @return See above.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public IQueryPrx getQueryService() throws ServiceException {
        return ExceptionHandler.of(gateway, g -> g.getQueryService(ctx))
                               .rethrow(DSOutOfServiceException.class,
                                        ServiceException::new,
                                        "Could not retrieve Query Service")
                               .get();
    }


    /**
     * Gets the {@link MetadataFacility} used to retrieve annotations from OMERO.
     *
     * @return See above.
     *
     * @throws ExecutionException If the MetadataFacility can't be retrieved or instantiated.
     */
    public MetadataFacility getMetadata() throws ExecutionException {
        return gateway.getFacility(MetadataFacility.class);
    }


    /**
     * Gets the {@link DataManagerFacility} to handle/write data on OMERO. A
     *
     * @return See above.
     *
     * @throws ExecutionException If the DataManagerFacility can't be retrieved or instantiated.
     */
    public DataManagerFacility getDm() throws ExecutionException {
        return gateway.getFacility(DataManagerFacility.class);
    }


    /**
     * Gets the {@link ROIFacility} used to manipulate ROIs from OMERO.
     *
     * @return See above.
     *
     * @throws ExecutionException If the ROIFacility can't be retrieved or instantiated.
     */
    public ROIFacility getRoiFacility() throws ExecutionException {
        return gateway.getFacility(ROIFacility.class);
    }


    /**
     * Gets the {@link TablesFacility} used to manipulate tables on OMERO.
     *
     * @return See above.
     *
     * @throws ExecutionException If the TablesFacility can't be retrieved or instantiated.
     */
    public TablesFacility getTablesFacility() throws ExecutionException {
        return gateway.getFacility(TablesFacility.class);
    }


    /**
     * Gets the {@link AdminFacility} to use admin specific function.
     *
     * @return See above.
     *
     * @throws ExecutionException If the AdminFacility can't be retrieved or instantiated.
     */
    public AdminFacility getAdminFacility() throws ExecutionException {
        return gateway.getFacility(AdminFacility.class);
    }


    /**
     * Creates or recycles the import store.
     *
     * @return See above.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public OMEROMetadataStoreClient getImportStore() throws ServiceException {
        storeUses.incrementAndGet();
        return ExceptionHandler.of(this, GatewayWrapper::getImportStoreLocked)
                               .rethrow(DSOutOfServiceException.class,
                                        ServiceException::new,
                                        "Could not retrieve import store")
                               .get();
    }


    /**
     * Closes the import store.
     */
    public void closeImport() {
        int remainingStores = storeUses.decrementAndGet();
        if (remainingStores <= 0) {
            closeImportStoreLocked();
        }
    }


    /**
     * Finds objects on OMERO through a database query.
     *
     * @param query The database query.
     *
     * @return A list of OMERO objects.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    public List<IObject> findByQuery(String query)
    throws ServiceException, OMEROServerError {
        return ExceptionHandler.of(gateway,
                                   g -> g.getQueryService(ctx)
                                         .findAllByQuery(query, null))
                               .handleServiceOrServer("Query failed: " + query)
                               .get();
    }


    /**
     * Saves an object on OMERO.
     *
     * @param object The OMERO object.
     *
     * @return The saved OMERO object
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public IObject save(IObject object)
    throws ServiceException, AccessException, ExecutionException {
        return call(getDm(),
                    d -> d.saveAndReturnObject(ctx, object),
                    "Cannot save object");
    }


    /**
     * Deletes an object from OMERO.
     *
     * @param object The OMERO object.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     Server error.
     * @throws InterruptedException If block(long) does not return.
     */
    void delete(IObject object)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        final long wait = 500L;
        ExceptionHandler.ofConsumer(getDm(),
                                    d -> d.delete(ctx, object).loop(10, wait))
                        .rethrow(InterruptedException.class)
                        .handleException("Cannot delete object")
                        .rethrow();
    }


    /**
     * Deletes multiple objects from OMERO.
     *
     * @param objects The OMERO objects.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     Server error.
     * @throws InterruptedException If block(long) does not return.
     */
    void delete(List<IObject> objects)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        final long wait = 500L;
        ExceptionHandler.ofConsumer(getDm(),
                                    d -> d.delete(ctx, objects).loop(10, wait))
                        .rethrow(InterruptedException.class)
                        .handleException("Cannot delete objects")
                        .rethrow();
    }


    /**
     * Deletes a file from OMERO
     *
     * @param id ID of the file to delete.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     Server error.
     * @throws InterruptedException If block(long) does not return.
     */
    public void deleteFile(Long id)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        deleteFiles(id);
    }


    /**
     * Deletes files from OMERO.
     *
     * @param ids List of files IDs to delete.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     Server error.
     * @throws InterruptedException If block(long) does not return.
     */
    public void deleteFiles(Long... ids)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        List<IObject> files = Arrays.stream(ids)
                                    .map(id -> new FileAnnotationI(id, false))
                                    .collect(Collectors.toList());
        delete(files);
    }


    /**
     * Overridden to return the host name, the group ID, the username and the connection status.
     *
     * @return See above.
     */
    @Override
    public String toString() {
        String host = ctx.getServerInformation() != null ? ctx.getServerInformation().getHost() : "null";
        return String.format("%s{host=%s, groupID=%d, userID=%d, connected=%b}",
                             getClass().getSimpleName(),
                             host,
                             ctx.getGroupID(),
                             user.getId(),
                             gateway.isConnected());
    }

}