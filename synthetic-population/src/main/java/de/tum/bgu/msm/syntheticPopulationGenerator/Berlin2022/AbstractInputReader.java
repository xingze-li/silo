package de.tum.bgu.msm.syntheticPopulationGenerator.Berlin2022;

abstract class AbstractInputReader {

    protected final DataSetSynPop dataSet;

    AbstractInputReader(DataSetSynPop dataSet) {
        this.dataSet = dataSet;
    }

    public abstract void read();
}
