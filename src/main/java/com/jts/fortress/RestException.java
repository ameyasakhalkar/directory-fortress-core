/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress;


/**
 * This exception extends {@link SecurityException} and is thrown when Fortress cannot call En Masse to perform a particular operation via RESTful interface.
 * See the {@link com.jts.fortress.constants.GlobalErrIds} javadoc for list of error ids.
 *
 * @author Shawn McKinney
 * @created February 10, 2012
 */
public class RestException extends SecurityException
{

    /**
     * Create an exception with an error code that maps to {@link com.jts.fortress.constants.GlobalErrIds} and message text.
     * @param  errorId see {@link com.jts.fortress.constants.GlobalErrIds} for list of valid error codes that can be set.  Valid values between 0 & 100_000.
     * @param msg contains textual information including method of origin and description of the root cause.
     */
    public RestException(int errorId, String msg)
    {
        super(errorId, msg);
    }

    /**
     * Create exception with error id, message and related exception.
     * @param  errorId see {@link com.jts.fortress.constants.GlobalErrIds} for list of valid error codes that can be set.  Valid values between 0 & 100_000.
     * @param msg contains textual information including method of origin and description of the root cause.
     * @param previousException contains reference to related exception which usually is system related, i.e. ldap.
     */
    public RestException(int errorId, String msg, Exception previousException)
    {
        super(errorId, msg, previousException);
    }
}