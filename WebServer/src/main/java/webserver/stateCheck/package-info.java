/**
 * This package contains a state checker for a FHEM model. Rules can be defined in a json file.
 * The state checker runs after parsing the FHEM model and writes the results in the sensors in the model.
 * The entry point is the {@link webserver.stateCheck.StateChecker#evaluate(webserver.fhemParser.fhemModel.FHEMModel, java.util.Optional)}  evaluate} method.
 * The StateChecker itself is a singleton which can be accessed with the {@link webserver.stateCheck.StateChecker#getInstance() getInstance} method.
 */
package webserver.stateCheck;
