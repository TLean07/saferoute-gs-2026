package br.com.saferoute;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionBO {
  private final RegionDAO dao;

  public RegionBO(RegionDAO dao) {
    this.dao = dao;
  }

  private void validate(Region v) {}

  @Transactional
  public Region create(Region v) {
    validate(v);
    return dao.create(v);
  }

  @Transactional
  public Region update(long id, Region v) {
    validate(v);
    return dao.update(id, v);
  }

  @Transactional
  public void delete(long id) {
    dao.delete(id);
  }
}
