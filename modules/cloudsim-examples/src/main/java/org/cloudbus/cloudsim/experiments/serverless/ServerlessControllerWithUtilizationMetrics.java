package org.cloudbus.cloudsim.experiments.serverless;


import org.cloudbus.cloudsim.serverless.ServerlessController;


public class ServerlessControllerWithUtilizationMetrics  extends ServerlessController {
    public ServerlessControllerWithUtilizationMetrics(String name, int overBookingfactor) throws Exception {
        super(name, overBookingfactor);
    }
}
