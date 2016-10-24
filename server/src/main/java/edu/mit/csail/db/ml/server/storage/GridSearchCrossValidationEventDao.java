package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.GridcellcrossvalidationRecord;
import jooq.sqlite.gen.tables.records.GridsearchcrossvalidationeventRecord;
import modeldb.CrossValidationEventResponse;
import modeldb.FitEventResponse;
import modeldb.GridSearchCrossValidationEvent;
import modeldb.GridSearchCrossValidationEventResponse;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GridSearchCrossValidationEventDao {
  public static GridSearchCrossValidationEventResponse store(GridSearchCrossValidationEvent gscve, DSLContext ctx) {
    gscve.bestFit.setExperimentRunId(gscve.experimentRunId);
    FitEventResponse fer = FitEventDao.store(gscve.bestFit.setProjectId(gscve.projectId).setProblemType(gscve.problemType), ctx);

    GridsearchcrossvalidationeventRecord gscveRec = ctx.newRecord(Tables.GRIDSEARCHCROSSVALIDATIONEVENT);
    gscveRec.setId(null);
    gscveRec.setNumfolds(gscve.numFolds);
    gscveRec.setBest(fer.fitEventId);
    gscveRec.setProject(gscve.projectId);
    gscveRec.setExperimentrun(gscve.experimentRunId);
    gscveRec.store();

    EventRecord ev = EventDao.store(gscveRec.getId(), "cross validation grid search", gscve.projectId, gscve.experimentRunId, ctx);

    gscve.crossValidations.forEach(cve -> cve.setDf(cve.df.setId(fer.dfId)));

    List<DataframeRecord> validationDfs = gscve
      .crossValidations
      .get(0)
      .folds
      .stream()
      .map(fold -> DataFrameDao.store(fold.validationDf, gscve.projectId, gscve.experimentRunId, ctx))
      .collect(Collectors.toList());

    List<DataframeRecord> trainingDfs = gscve
      .crossValidations
      .get(0)
      .folds
      .stream()
      .map(fold -> DataFrameDao.store(fold.trainingDf, gscve.projectId, gscve.experimentRunId, ctx))
      .collect(Collectors.toList());

    List<CrossValidationEventResponse> cveResponses = gscve
      .crossValidations
      .stream()
      .map(cve -> {
        IntStream
          .range(0, cve.folds.size())
          .forEach(ind ->
            cve.folds.get(ind)
              .setValidationDf(cve.folds.get(ind).validationDf.setId(validationDfs.get(ind).getId()))
              .setTrainingDf(cve.folds.get(ind).trainingDf.setId(trainingDfs.get(ind).getId()))
          );
        return CrossValidationEventDao.store(cve.setProblemType(gscve.problemType), ctx);
      })
      .collect(Collectors.toList());

    cveResponses
      .forEach(cver -> {
        GridcellcrossvalidationRecord rec = ctx.newRecord(Tables.GRIDCELLCROSSVALIDATION);
        rec.setId(null);
        rec.setGridsearch(gscveRec.getId());
        rec.setCrossvalidation(cver.getCrossValidationEventId());
        rec.setProject(gscve.projectId);
        rec.setExperimentrun(gscve.experimentRunId);
        rec.store();
        rec.getId();
      });

    return new GridSearchCrossValidationEventResponse(gscveRec.getId(), ev.getId(), fer, cveResponses);
  }
}