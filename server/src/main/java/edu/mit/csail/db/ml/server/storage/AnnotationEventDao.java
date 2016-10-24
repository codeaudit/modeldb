package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.AnnotationRecord;
import jooq.sqlite.gen.tables.records.AnnotationfragmentRecord;
import modeldb.AnnotationEvent;
import modeldb.AnnotationEventResponse;
import modeldb.AnnotationFragment;
import modeldb.AnnotationFragmentResponse;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AnnotationEventDao {
  private static final String DATAFRAME_TYPE = "dataframe";
  private static final String SPEC_TYPE = "spec";
  private static final String TRANSFORMER_TYPE = "transformer";
  private static final String MESSAGE_TYPE = "message";

  public static AnnotationEventResponse store(AnnotationEvent ae, DSLContext ctx) {
    AnnotationRecord aRec = ctx.newRecord(Tables.ANNOTATION);
    aRec.setId(null);
    aRec.setPosted(new Timestamp((new Date()).getTime()));
    aRec.setProject(ae.projectId);
    aRec.setExperimentrun(ae.experimentRunId);
    aRec.store();

    List<Integer> fragmentIds = ae
      .fragments
      .stream()
      .map(frag -> {
        switch (frag.type) {
          case DATAFRAME_TYPE:
            return DataFrameDao.store(frag.df, ae.projectId, ae.experimentRunId, ctx).getId();
          case SPEC_TYPE:
            return TransformerSpecDao.store(frag.spec, ae.projectId, ae.experimentRunId, ctx).getId();
          case TRANSFORMER_TYPE:
            return TransformerDao.store(frag.transformer, ae.projectId, ae.experimentRunId, ctx).getId();
          default:
            return -1;
        }
      })
      .collect(Collectors.toList());

    List<AnnotationFragmentResponse> fragResp = IntStream
      .range(0, fragmentIds.size())
      .mapToObj(ind -> {
        AnnotationfragmentRecord afRec = ctx.newRecord(Tables.ANNOTATIONFRAGMENT);
        AnnotationFragment frag = ae.fragments.get(ind);
        afRec.setId(null);
        afRec.setAnnotation(aRec.getId());
        afRec.setFragmentindex(ind);
        afRec.setType(frag.type);
        afRec.setProject(ae.projectId);
        afRec.setExperimentrun(ae.experimentRunId);
        afRec.setTransformer(frag.type.equals(TRANSFORMER_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setDataframe(frag.type.equals(DATAFRAME_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setSpec(frag.type.equals(SPEC_TYPE) ? fragmentIds.get(ind) : null);
        afRec.setMessage(frag.type.equals(MESSAGE_TYPE) ? frag.message : null);
        afRec.store();
        afRec.getId();
        return new AnnotationFragmentResponse(frag.type, fragmentIds.get(ind));
      })
      .collect(Collectors.toList());

    return new AnnotationEventResponse(aRec.getId(), fragResp);
  }
}