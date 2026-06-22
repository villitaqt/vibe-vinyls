package com.vivevinyls.catalogo;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

/**
 * Criterios de búsqueda y filtrado del catálogo, cada uno opcional. Se combinan
 * con {@code and} en el servicio. Las que cruzan los puentes N:M marcan la query
 * como {@code distinct} para no duplicar vinilos con varios artistas/géneros.
 */
final class ViniloSpecifications {

    private ViniloSpecifications() {
    }

    /** Texto libre sobre título del vinilo o nombre de alguno de sus artistas (RF-07). */
    static Specification<Vinilo> texto(String q) {
        if (!StringUtils.hasText(q)) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> va = root.join("artistas", JoinType.LEFT);
            Join<Object, Object> artista = va.join("artista", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("titulo")), patron),
                    cb.like(cb.lower(artista.get("nombre")), patron));
        };
    }

    /** Filtro por nombre de género (insensible a may/min). */
    static Specification<Vinilo> genero(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            return null;
        }
        String valor = nombre.trim().toLowerCase();
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> vg = root.join("generos", JoinType.INNER);
            Join<Object, Object> genero = vg.join("genero", JoinType.INNER);
            return cb.equal(cb.lower(genero.get("nombre")), valor);
        };
    }

    /** Filtro por nombre de artista (insensible a may/min). */
    static Specification<Vinilo> artista(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            return null;
        }
        String valor = nombre.trim().toLowerCase();
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> va = root.join("artistas", JoinType.INNER);
            Join<Object, Object> artista = va.join("artista", JoinType.INNER);
            return cb.equal(cb.lower(artista.get("nombre")), valor);
        };
    }

    /** Filtro por año de edición. */
    static Specification<Vinilo> anio(Integer anio) {
        if (anio == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("anio"), anio);
    }

    /** Filtro por nombre de sello (insensible a may/min). */
    static Specification<Vinilo> sello(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            return null;
        }
        String valor = nombre.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("sello").get("nombre")), valor);
    }
}
