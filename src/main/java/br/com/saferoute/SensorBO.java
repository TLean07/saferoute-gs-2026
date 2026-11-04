package br.com.saferoute;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensorBO {
  private final SensorDAO dao;
  private final SegmentDAO segmentDAO;

  public SensorBO(SensorDAO dao, SegmentDAO segmentDAO) {
    this.dao = dao;
    this.segmentDAO = segmentDAO;
  }

  private void validate(Sensor v) {
    segmentDAO.get(v.segmentId());
  }

  @Transactional
  public Sensor create(Sensor v) {
    validate(v);
    return dao.create(v);
  }

  @Transactional
  public Sensor update(long id, Sensor v) {
    validate(v);
    return dao.update(id, v);
  }

  @Transactional
  public void delete(long id) {
    dao.delete(id);
  }
}
