package com.rapidminer.extension.anomalydetection.operator;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.model.AnomalyDetectionModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.tools.Ontology;


public abstract class AbstractAnomalyOperator extends Operator implements CapabilityProvider {
    protected InputPort exaInput = getInputPorts().createPort("exa", ExampleSet.class);

    protected OutputPort exaOutput = getOutputPorts().createPort("exa");
    protected OutputPort modOutput = getOutputPorts().createPort("mod");

    public AbstractAnomalyOperator(OperatorDescription description) {
        super(description);
        getTransformer().addGenerationRule(modOutput, AnomalyDetectionModel.class);

        getTransformer().addRule(new MDTransformationRule() {
            @Override
            public void transformMD() {
                try {
                    TableMetaData tmd = exaInput.getMetaData(TableMetaData.class);
                    TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
                    ColumnInfoBuilder columnInfoBuilder =
                            new ColumnInfoBuilder(ColumnType.REAL);
                    builder.add(AnomalyUtilities.ANOMALY_SCORE_NAME, columnInfoBuilder.build()).addColumnMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME, ColumnRole.SCORE);
                    exaOutput.deliverMD(builder.build());
                } catch (IncompatibleMDClassException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean supportsCapability(OperatorCapability capability) {
        switch (capability) {
            case NUMERICAL_LABEL:
            case NUMERICAL_ATTRIBUTES:
            case ONE_CLASS_LABEL:
            case NO_LABEL:
            case POLYNOMINAL_LABEL:
            case BINOMINAL_LABEL:
                return true;
            default:
                return false;
        }
    }
}
