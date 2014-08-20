package org.jitsi.service.fileaccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.jitsi.util.Logger;

/**
 * TransactionBasedFile.
 * <p>
 * Provides safe write access to an underlying file.  Safety is ensured by all
 * writes going to a temp file, which is then moved over the original when <tt>
 * commitTransaction</tt> is called.
 * <p>
 * Usage:<br>
 * <ul>
 * <li>Create a TransactionBasedFile for the file</li>
 * <li>Call <tt>beginTransaction</tt></li>
 * <li>Call <tt>getOutputStream</tt> and write to the returned stream</li>
 * <li>If happy with the changes, call <tt>commitTransaction</tt> to save the
 * result</li>
 * <li>If unhappy, call <tt>abortTransaction</tt> to discard the changes</li>
 * </ul>
 */
public class TransactionBasedFile
{
    private static final String TEMP_SUFFIX = ".tmp";

    private static final Logger logger = Logger.getLogger(TransactionBasedFile.class);

    private File mTempFile;
    private File mFile;
    private boolean transactionInProgress;

    /**
     * Create a new TransactionBasedFile
     * @param file The file to wrap
     */
    public TransactionBasedFile(File file)
    {
        mFile = file;
        logger.debug("Create transaction-based file: " + file.getAbsolutePath());
        transactionInProgress = false;
    }

    /**
     * Start a new transaction.  Must be closed by <tt>commitTransaction</tt> or
     * <tt>abortTransaction</tt> before the next transaction can begin.  User
     * can now call <tt>getOutputStream</tt> and write data.
     */
    public synchronized void beginTransaction()
    {
        if (transactionInProgress)
        {
            logger.error("Transaction already in progress");
            throw new IllegalStateException("Transaction already in progress");
        }

        transactionInProgress = true;
        mTempFile = new File(mFile.getAbsolutePath() + TEMP_SUFFIX);
    }

    /**
     * Save all writes to the underlying file and end the transaction.
     */
    public synchronized void commitTransaction()
    {
        if (!transactionInProgress)
        {
            logger.error("No transaction to commit");
            throw new IllegalStateException("No transaction to commit");
        }

        try
        {
            Files.move(mTempFile.toPath(),
                       mFile.toPath(),
                       StandardCopyOption.ATOMIC_MOVE);
        }
        catch (IOException ioex)
        {
            logger.error("Couldn't commit transaction", ioex);
        }

        mTempFile = null;
        transactionInProgress = false;
    }

    /**
     * Abandon all writes in this transaction, and end the transaction.
     */
    public synchronized void abortTransaction()
    {
        if (!transactionInProgress)
        {
            logger.error("No transaction to abort");
            throw new IllegalStateException("No transaction to abort");
        }

        mTempFile.delete();
        mTempFile = null;

        transactionInProgress = false;
    }

    /**
     * Obtain an OutputStream to which changes can be written.  Can only be
     * called while a transaction is active.
     * @return The OutputStream.  <b>Caller is responsible for closing this.</b>
     */
    public synchronized OutputStream getOutputStream()
    {
        if (!transactionInProgress)
        {
            logger.error("No transaction - not possible to get output stream");
            throw new IllegalStateException("No transaction - not possible to get output stream");
        }

        try
        {
            return new FileOutputStream(mTempFile);
        }
        catch (FileNotFoundException e)
        {
            logger.error("Couldn't get file output stream for temp file: " +  mTempFile);
            throw new IllegalStateException(
                    "Couldn't get file output stream for temp file", e);
        }
    }
}
