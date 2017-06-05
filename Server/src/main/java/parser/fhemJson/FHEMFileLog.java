package parser.fhemJson;

/**
 * @author Rafael
 */

public class FHEMFileLog extends FHEMDevice {
    public FHEMFileLog(FHEMDevice fhemDevice) {
        boolean hasConfigfile = fhemDevice.getInternals().getCurrentLogfileField().isPresent();
        boolean showInApp = fhemDevice.isShowInApp();
    }
}
