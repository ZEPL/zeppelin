package com.nflabs.zeppelin.driver.elasticsearch;

import com.nflabs.zeppelin.result.ColumnDef;
import com.nflabs.zeppelin.result.Result;

public class ESResult extends Result {
	public ESResult(ColumnDef[] createColumnDef) {
		super(createColumnDef);
	}

	Object rawData;
}
