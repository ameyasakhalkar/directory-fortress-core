/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.rbac;

import com.jts.fortress.constants.GlobalErrIds;
import com.jts.fortress.util.time.Validator;
import com.jts.fortress.util.time.Constraint;
import com.jts.fortress.util.time.Time;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This class performs Dynamic Separation of Duty checking on a collection of roles targeted for
 * activation within a particular user's session.  This method is called from {@link com.jts.fortress.util.time.CUtil#validateConstraints} during createSession
 * sequence for users.  If DSD constraint violation is detected for a particular role method will remove the role
 * from collection of activation candidates and log a warning.  This proc will also consider hierarchical relations
 * between roles (RBAC spec calls these authorized roles).
 * This validator will ensure the role being targeted for activation does not violate RBAC dynamic separation of duty constraints.
 * <h4> Constraint Targets include</h4>
 * <ol>
 * <li>{@link com.jts.fortress.rbac.User} maps to 'ftCstr' attribute on 'ftUserAttrs' object class</li>
 * <li>{@link com.jts.fortress.rbac.UserRole} maps to 'ftRC' attribute on 'ftUserAttrs' object class</li>
 * <li>{@link com.jts.fortress.rbac.Role}  maps to 'ftCstr' attribute on 'ftRls' object class</li>
 * <li>{@link com.jts.fortress.arbac.AdminRole}  maps to 'ftCstr' attribute on 'ftRls' object class</li>
 * <li>{@link com.jts.fortress.arbac.UserAdminRole}  maps to 'ftARC' attribute on 'ftRls' object class</li>
 * </ol>
 * </p>
 *
 * @author smckinn
 * @created September 12, 2010
 */
public class DSD
    implements Validator
{
    private static final String OCLS_NM = DSD.class.getName();
    private static final Logger log = Logger.getLogger(OCLS_NM);
    private static final SdP sp = new SdP();

    /**
     * This method is called during entity activation, {@link com.jts.fortress.util.time.CUtil#validateConstraints} and ensures the role does not violate dynamic separation of duty constraints.
     *
     * @param session    contains list of RBAC roles {@link com.jts.fortress.rbac.UserRole} targeted for activation.
     * @param constraint required for Validator interface, not used here..
     * @param time       required for Validator interface, not used here.
     * @return '0' if validation succeeds else {@link com.jts.fortress.constants.GlobalErrIds#ACTV_FAILED_DSD} if failed.
     */
    public int validate(Session session, Constraint constraint, Time time) throws com.jts.fortress.SecurityException
    {
        int rc = 0;
        int matchCount;

        // get all candidate activated roles user:
        List<UserRole> activeRoleList = session.getRoles();
        if (activeRoleList == null || activeRoleList.size() == 0)
        {
            return rc;
        }
        // get the list of authorized roles for this user:
        Set<String> authorizedRoleSet = RoleUtil.getInheritedRoles(activeRoleList);
        if (authorizedRoleSet != null && authorizedRoleSet.size() > 0)
        {
            // get all DSD sets that contain the candidate activated and authorized roles:
            List<SDSet> dsdSets = sp.search(authorizedRoleSet, SDSet.SDType.DYNAMIC);
            if (dsdSets != null && dsdSets.size() > 0)
            {
                Iterator activatedRoles = activeRoleList.iterator();
                for (SDSet dsd : dsdSets)
                {
                    matchCount = 0;
                    Set<String> map = dsd.getMembers();

                    // now check the DSD on every role activation candidate contained within session object:
                    while (activatedRoles.hasNext())
                    {
                        UserRole activatedRole = (UserRole) activatedRoles.next();
                        //if (map.containsKey(activatedRole.getName()))
                        if (map.contains(activatedRole.getName()))
                        {
                            matchCount++;
                            if (matchCount >= dsd.getCardinality())
                            {
                                activatedRoles.remove();
                                String warning = OCLS_NM + ".validate userId [" + session.getUserId() + "] failed activation of assignedRole [" + activatedRole.getName() + "] validates DSD Set Name:" + dsd.getName() + " Cardinality:" + dsd.getCardinality() + ", Count:" + matchCount;
                                log.warn(warning);
                                rc = GlobalErrIds.ACTV_FAILED_DSD;
                            }
                        }
                        else
                        {
                            Set<String> parentSet = RoleUtil.getAscendants(activatedRole.getName());
                            // now check for every role inherited from this activated role:
                            for (String parentRole : parentSet)
                            {
                                //if (map.containsKey(parentRole))
                                if (map.contains(parentRole))
                                {
                                    matchCount++;
                                    if (matchCount >= dsd.getCardinality())
                                    {
                                        // remove the assigned role from session (not the authorized role):
                                        activatedRoles.remove();
                                        String warning = OCLS_NM + ".validate userId [" + session.getUserId() + "] assignedRole [" + activatedRole.getName() + "] parentRole [" + parentRole + "] validates DSD Set Name:" + dsd.getName() + " Cardinality:" + dsd.getCardinality() + ", Count:" + matchCount;
                                        log.warn(warning);
                                        rc = GlobalErrIds.ACTV_FAILED_DSD;
                                    }
                                    // Breaking out of the loop here means the DSD algorithm will only match one
                                    // role per parent.
                                    break;
                                }
                            }

                        }
                    }
                }
            }
        }
        return rc;
    }
}