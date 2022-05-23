package com.rapidminer.extension.anomalydetection.model;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;

@Deprecated
public abstract class IOTableAnomalyModel extends AbstractModel {



	private static final long serialVersionUID = 1085847259711014431L;

	/**
	 * Created a new model which was built on the given example set. Please note that the given example set is automatically
	 * transformed into a {@link HeaderExampleSet} which means that no reference to the data itself is kept but only to the
	 * header, i.e. to the attribute meta descriptions.
	 *
	 * @param exampleSet
	 */
	protected IOTableAnomalyModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	@Override
	public ExampleSet apply(ExampleSet testSet) throws OperatorException {
		ConcurrencyContext context = new SequentialConcurrencyContext();
		Table ioTable = BeltConverter.convert(testSet, context).getTable();
		IOTable resultTable = apply(ioTable);
		return BeltConverter.convert(resultTable, context);
	}

	/**
	 * Score a given table. This is preferred since you do not need to convert ExampleSets to Tables
	 * @param table the table to score
	 * @return a scored table.
	 */
	public abstract IOTable apply(Table table);


}