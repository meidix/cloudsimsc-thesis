package org.cloudbus.cloudsim.experiments.ensure;

import org.cloudbus.cloudsim.container.core.Container;
import org.cloudbus.cloudsim.container.core.ContainerVm;
import org.cloudbus.cloudsim.serverless.Constants;
import org.cloudbus.cloudsim.serverless.FunctionScheduler;
import org.cloudbus.cloudsim.serverless.ServerlessInvoker;

import java.util.HashMap;

public class EnsureFunctionScheduler  extends FunctionScheduler {


    public EnsureFunctionScheduler() {
        super();
    }

    public ContainerVm findVmForContainer(Container container) {
        ServerlessInvoker selectedVm = null;
        for (int i=0; i <= getContainerVmList().size(); i++ ) {
            ServerlessInvoker currentVm = (ServerlessInvoker) getContainerVmList().get(i);
        }

        return selectedVm;
    }

    public int calculateContainerVmState(ContainerVm containerVm) {
        return Constants.ENSURE_INVOKER_STATE_SAFE;
    }


}
