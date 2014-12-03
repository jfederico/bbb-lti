package org.bigbluebutton

import java.util.Date;

class LtiRegistration {
    // Auto Timestamp
    Date dateCreated
    Date lastUpdated

    String reg_key
    String reg_password
    String launch_presentation_return_url
    String tc_profile_url

    static constraints = {
    }
}
