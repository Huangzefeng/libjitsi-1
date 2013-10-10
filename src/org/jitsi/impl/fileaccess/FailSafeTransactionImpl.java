/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.impl.fileaccess;

import java.io.*;

import org.jitsi.impl.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.*;

/**
 * A failsafe transaction class. By failsafe we mean here that the file
 * concerned always stays in a coherent state. This class use the transactional
 * model.
 *
 * @author Benoit Pradelle
 */
public class FailSafeTransactionImpl
    implements FailSafeTransaction
{
    private static final Logger logger
        = Logger.getLogger(FailSafeTransactionImpl.class);

    /**
     * Original file used by the transaction
     */
    private File file;

    /**
     * Backup file used by the transaction
     */
    private File backup;

    /**
     * Extension of a partial file
     */
    private static final String PART_EXT = ".part";

    /**
     * Extension of a backup copy
     */
    private static final String BAK_EXT = ".bak";

    /**
     * Creates a new transaction.
     *
     * @param file The file associated with this transaction
     *
     * @throws NullPointerException if the file is null
     */
    protected FailSafeTransactionImpl(File file)
        throws NullPointerException
    {
        if (file == null) {
            throw new NullPointerException("null file provided");
        }

        this.file = file;
        this.backup = null;
    }

    /**
     * Ensure that the file accessed is in a coherent state. This function is
     * useful to do a failsafe read without starting a transaction.
     *
     * @throws IllegalStateException if the file doesn't exists anymore
     * @throws IOException if an IOException occurs during the file restoration
     */
    public synchronized void restoreFile()
        throws IllegalStateException, IOException
    {
        File back = new File(this.file.getAbsolutePath() + BAK_EXT);

        // if a backup copy is still present, simply restore it
        if (back.exists()) {
            failsafeCopy(back.getAbsolutePath(),
                    this.file.getAbsolutePath());

            back.delete();
        }
    }

    /**
     * Begins a new transaction. If a transaction is already active, commits the
     * changes and begin a new transaction.
     * A transaction can be closed by a commit or rollback operation.
     * When the transaction begins, the file is restored to a coherent state if
     * needed.
     *
     * @throws IllegalStateException if the file doesn't exists anymore
     * @throws IOException if an IOException occurs during the transaction
     * creation
     */
    public synchronized void beginTransaction()
        throws IllegalStateException, IOException
    {
        // if the last transaction hasn't been closed, commit it
        if (this.backup != null) {
            this.commit();
        }

        // if needed, restore the file in its previous state
        restoreFile();

        this.backup = new File(this.file.getAbsolutePath() + BAK_EXT);

        // else backup the current file
        failsafeCopy(this.file.getAbsolutePath(),
                this.backup.getAbsolutePath());
    }

    /**
     * Closes the transaction and commit the changes. Everything written in the
     * file during the transaction is saved.
     *
     * @throws IllegalStateException if the file doesn't exists anymore
     * @throws IOException if an IOException occurs during the operation
     */
    public synchronized void commit()
        throws IllegalStateException, IOException
    {
        if (this.backup == null) {
            return;
        }

        // simply delete the backup file
        this.backup.delete();
        this.backup = null;
    }

    /**
     * Closes the transation and cancel the changes. Everything written in the
     * file during the transaction is NOT saved.
     * @throws IllegalStateException if the file doesn't exists anymore
     * @throws IOException if an IOException occurs during the operation
     */
    public synchronized void rollback()
        throws IllegalStateException, IOException
    {
        logger.warn("Failsafe transaction rolling back " + file);

        if (this.backup == null) {
            logger.error("Could not roll back - no backup found!");
            return;
        }

        // restore the backup and delete it
        failsafeCopy(this.backup.getAbsolutePath(),
                this.file.getAbsolutePath());
        this.backup.delete();
        this.backup = null;
        logger.info("Rollback of " + file + " completed and backup removed.");
    }

    /**
     * Copy a file in a fail-safe way. The destination is created in an atomic
     * way.
     *
     * @param from The file to copy
     * @param to The copy to create
     *
     * @throws IllegalStateException if the file doesn't exists anymore
     * @throws IOException if an IOException occurs during the operation
     */
    private synchronized void failsafeCopy(String from, String to)
        throws IllegalStateException, IOException
    {
        logger.trace("Beginning failsafe copy from " + from + " to " + to);
        FileInputStream in = null;
        FileOutputStream out = null;

        File ptoF = new File(to + PART_EXT);
        if (ptoF.exists()) {
            ptoF.delete();
        }

        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to + PART_EXT);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e.getMessage());
        }

        // actually copy the file
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
          out.write(buf, 0, len);
        }

        in.close();
        out.close();

        // to ensure a perfect copy, delete the destination if it exists
        File toF = new File(to);
        if (toF.exists()) {
            logger.debug("Overwriting file at " + to + " for failsafe copy.");
            boolean success = toF.delete();
            if (!success)
                logger.error("Failed to delete file at " + to + " during " +
                             "failsafe copy.");
        }

        // once done, rename the partial file to the final copy
        ptoF.renameTo(toF);

        logger.trace("Failsafe copy from " + from + " to " + to + " succeeded");
    }
}
