package br.com.saferoute;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
@Service public class SegmentBO {
 private final SegmentDAO dao; private final PointDAO pointDAO;
 public SegmentBO(SegmentDAO dao, PointDAO pointDAO) {this.dao=dao;this.pointDAO=pointDAO;}
 private void validate(Segment v) { pointDAO.get(v.fromId());pointDAO.get(v.toId());if(v.fromId().equals(v.toId()))throw new IllegalArgumentException("Um trecho precisa ligar pontos diferentes"); if(!pointDAO.get(v.fromId()).regionId().equals(pointDAO.get(v.toId()).regionId()))throw new IllegalArgumentException("Pontos devem pertencer à mesma região"); }
 @Transactional public Segment create(Segment v) {validate(v);return dao.create(v);}
 @Transactional public Segment update(long id,Segment v) {validate(v);return dao.update(id,v);}
 @Transactional public void delete(long id) {dao.delete(id);}
}
