package com.rapidminer.extension.anomalydetection.operator.time_series;

import java.util.Arrays;
import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.NumericRowWriter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.extension.anomalydetection.utility.MASS;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;


public class MASSOperator extends Operator {
	InputPort exaInput = getInputPorts().createPort("exa");
	OutputPort exaOut = getOutputPorts().createPort("exa");

	public static final String PARAMETER_ATTRIBUTE="attribute";
	public static final String PARAMETER_LENGTH="length";
	public MASSOperator(OperatorDescription description) {
		super(description);
	}

	public void doWork() throws UserError {
		IOTable ioTable = exaInput.getData(IOTable.class);

		double[] data = getTimeSeries(ioTable.getTable());
		int length = getParameterAsInt(PARAMETER_LENGTH);
		TableBuilder builder = Builders.newTableBuilder(ioTable.getTable());
		for(int startvalue = 0; startvalue < data.length-length;startvalue++) {
			double[] sequence = Arrays.copyOfRange(data, startvalue, startvalue+length);
			MASS m  = new MASS();
			double[] d = m.findNN(data, sequence, data.length, sequence.length);
			NumericBuffer numericBuffer = Buffers.realBuffer(data.length);

			for (int i = 0; i < d.length; i++) {
				numericBuffer.set(i, d[i]);
			}

			builder.add(Integer.toString(startvalue), numericBuffer.toColumn());
		}
		exaOut.deliver(new IOTable(builder.build(new SequentialContext())));

	}

	double[] getTimeSeries(Table table) throws UndefinedParameterError {
		Column c = table.column(getParameterAsString(PARAMETER_ATTRIBUTE));
		double[] data = new double[c.size()];
		c.fill(data,0);
		return data;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE, "attribute", exaInput));
		types.add(new ParameterTypeInt(PARAMETER_LENGTH,"length",1,Integer.MAX_VALUE,12));
		return types;
	}
}
