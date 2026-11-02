package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class VendaService {
    private final VendaRepository repository;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final InstrumentoRepository instrumentoRepository;
    private final EntityManager entityManager;

    public VendaService(VendaRepository repository, ClienteRepository clienteRepository,
                        ColaboradorRepository colaboradorRepository, InstrumentoRepository instrumentoRepository,
                        EntityManager entityManager) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.instrumentoRepository = instrumentoRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Venda cadastrar(Long clienteId, Long vendedorId) {
        Cliente cliente = clienteRepository.findOneById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Colaborador vendedor = colaboradorRepository.findOneById(vendedorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Colaborador não encontrado"));
        validarParticipantes(cliente, vendedor);
        return repository.save(new Venda(cliente, vendedor, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Venda buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional
    public Venda adicionarItem(Long vendaId, Long instrumentoId, BigDecimal quantidade) {
        Venda venda = buscarEntidade(vendaId);
        Instrumento instrumento = instrumentoRepository.findById(instrumentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Instrumento não encontrado"));
        if (instrumento.getStatus() != Status.ATIVO) throw new IllegalArgumentException("Instrumento deve estar ativo");
        venda.adicionarItem(instrumento, quantidade);
        return venda;
    }

    @Transactional
    public Venda removerItem(Long vendaId, Long itemId) {
        Venda venda = buscarEntidade(vendaId);
        venda.removerItem(itemId);
        return venda;
    }

    @Transactional
    public Venda cancelar(Long id) {
        Venda venda = buscarEntidade(id);
        venda.cancelar();
        return venda;
    }

    @Transactional
    public Venda finalizar(Long id) {
        // A projeção consulta o estado atual e bloqueia somente a venda, sem o EntityGraph
        // nem o refresh em cascata dos itens. A entidade já gerenciada pode estar desatualizada.
        StatusVenda statusAtual = entityManager.createQuery(
                        "select v.status from Venda v where v.id = :id", StatusVenda.class)
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList().stream()
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Venda não encontrada"));
        if (statusAtual != StatusVenda.ABERTA) {
            throw new IllegalStateException("Somente vendas abertas podem ser alteradas");
        }
        Venda venda = buscarEntidade(id);
        validarParticipantes(venda.getCliente(), venda.getVendedor());
        List<ItemVenda> itensOrdenados = venda.getItens().stream()
                .sorted(Comparator.comparing(item -> item.getInstrumento().getId()))
                .toList();
        for (ItemVenda item : itensOrdenados) {
            Instrumento instrumento = item.getInstrumento();
            entityManager.refresh(instrumento, LockModeType.PESSIMISTIC_WRITE);
            if (instrumento.getStatus() != Status.ATIVO) {
                throw new IllegalArgumentException("Todos os instrumentos da venda devem estar ativos");
            }
            instrumento.retirarEstoque(item.getQuantidade().intValueExact());
        }
        venda.finalizar();
        return venda;
    }

    @Transactional(readOnly = true)
    public Page<Venda> pesquisar(StatusVenda status, Long clienteId, Long vendedorId,
                                LocalDate dataInicial, LocalDate dataFinal, Pageable pageable) {
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new IllegalArgumentException("Data inicial não pode ser posterior à data final");
        }
        pageable.getSort().forEach(ordem -> {
            if (!Set.of("id", "dataVenda", "status").contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        });
        Specification<Venda> filtros = (root, query, cb) -> cb.conjunction();
        if (status != null) filtros = filtros.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (clienteId != null) filtros = filtros.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), clienteId));
        if (vendedorId != null) filtros = filtros.and((root, query, cb) -> cb.equal(root.get("vendedor").get("id"), vendedorId));
        if (dataInicial != null) filtros = filtros.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataVenda"), dataInicial.atStartOfDay()));
        if (dataFinal != null) filtros = filtros.and((root, query, cb) -> cb.lessThan(root.get("dataVenda"), dataFinal.plusDays(1).atStartOfDay()));
        return repository.findAll(filtros, pageable);
    }

    private Venda buscarEntidade(Long id) {
        return repository.findOneById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Venda não encontrada"));
    }

    private void validarParticipantes(Cliente cliente, Colaborador vendedor) {
        if (cliente.getPessoa().getStatus() != Status.ATIVO) throw new IllegalArgumentException("Pessoa do cliente deve estar ativa");
        if (vendedor.getPessoa().getStatus() != Status.ATIVO) throw new IllegalArgumentException("Pessoa do vendedor deve estar ativa");
        if (vendedor.getFuncao() != FuncaoColaborador.VENDEDOR && vendedor.getFuncao() != FuncaoColaborador.GERENTE) {
            throw new IllegalArgumentException("Colaborador deve exercer a função de vendedor ou gerente");
        }
    }
}
