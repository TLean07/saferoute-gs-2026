package br.com.saferoute;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service public class PointBO {
 private final PointDAO dao; private final RegionDAO regionDAO;
 public PointBO(PointDAO dao, RegionDAO regionDAO) {this.dao=dao;this.regionDAO=regionDAO;}
 private void validate(Point v) { regionDAO.get(v.regionId()); }
 @Transactional public Point create(Point v) {validate(v);return dao.create(v);}
 @Transactional public Point update(long id,Point v) {validate(v);return dao.update(id,v);}
 @Transactional public void delete(long id) {dao.delete(id);}
}
