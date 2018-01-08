/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.vectorized;

import org.apache.spark.sql.catalyst.util.MapData;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.unsafe.types.UTF8String;

/**
 * An interface representing in-memory columnar data in Spark. This interface defines the main APIs
 * to access the data, as well as their batched versions. The batched versions are considered to be
 * faster and preferable whenever possible.
 *
 * Most of the APIs take the rowId as a parameter. This is the batch local 0-based row id for values
 * in this ColumnVector.
 *
 * ColumnVector supports all the data types including nested types. To handle nested types,
 * ColumnVector can have children and is a tree structure. For struct type, it stores the actual
 * data of each field in the corresponding child ColumnVector, and only stores null information in
 * the parent ColumnVector. For array type, it stores the actual array elements in the child
 * ColumnVector, and stores null information, array offsets and lengths in the parent ColumnVector.
 *
 * ColumnVector is expected to be reused during the entire data loading process, to avoid allocating
 * memory again and again.
 *
 * ColumnVector is meant to maximize CPU efficiency but not to minimize storage footprint.
 * Implementations should prefer computing efficiency over storage efficiency when design the
 * format. Since it is expected to reuse the ColumnVector instance while loading data, the storage
 * footprint is negligible.
 */
public abstract class ColumnVector implements AutoCloseable {

  /**
   * Returns the data type of this column vector.
   */
  public final DataType dataType() { return type; }

  /**
   * Cleans up memory for this column. The column is not usable after this.
   */
  public abstract void close();

  /**
   * Returns the number of nulls in this column.
   */
  public abstract int numNulls();

  /**
   * Returns whether the value at rowId is NULL.
   */
  public abstract boolean isNullAt(int rowId);

  /**
   * Returns the value for rowId.
   */
  public abstract boolean getBoolean(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract boolean[] getBooleans(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract byte getByte(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract byte[] getBytes(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract short getShort(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract short[] getShorts(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract int getInt(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract int[] getInts(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract long getLong(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract long[] getLongs(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract float getFloat(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract float[] getFloats(int rowId, int count);

  /**
   * Returns the value for rowId.
   */
  public abstract double getDouble(int rowId);

  /**
   * Gets values from [rowId, rowId + count)
   */
  public abstract double[] getDoubles(int rowId, int count);

  /**
   * Returns the length of the array for rowId.
   */
  public abstract int getArrayLength(int rowId);

  /**
   * Returns the offset of the array for rowId.
   */
  public abstract int getArrayOffset(int rowId);

  /**
   * Returns the struct for rowId.
   */
  public final ColumnarRow getStruct(int rowId) {
    return new ColumnarRow(this, rowId);
  }

  /**
   * A special version of {@link #getStruct(int)}, which is only used as an adapter for Spark
   * codegen framework, the second parameter is totally ignored.
   */
  public final ColumnarRow getStruct(int rowId, int size) {
    return getStruct(rowId);
  }

  /**
   * Returns the array for rowId.
   */
  public final ColumnarArray getArray(int rowId) {
    return new ColumnarArray(arrayData(), getArrayOffset(rowId), getArrayLength(rowId));
  }

  /**
   * Returns the map for rowId.
   */
  public MapData getMap(int ordinal) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the decimal for rowId.
   */
  public abstract Decimal getDecimal(int rowId, int precision, int scale);

  /**
   * Returns the UTF8String for rowId. Note that the returned UTF8String may point to the data of
   * this column vector, please copy it if you want to keep it after this column vector is freed.
   */
  public abstract UTF8String getUTF8String(int rowId);

  /**
   * Returns the byte array for rowId.
   */
  public abstract byte[] getBinary(int rowId);

  /**
   * Returns the data for the underlying array.
   */
  public abstract ColumnVector arrayData();

  /**
   * Returns the ordinal's child data column.
   */
  public abstract ColumnVector getChildColumn(int ordinal);

  /**
   * Data type for this column.
   */
  protected DataType type;

  /**
   * Sets up the common state and also handles creating the child columns if this is a nested
   * type.
   */
  protected ColumnVector(DataType type) {
    this.type = type;
  }
}
