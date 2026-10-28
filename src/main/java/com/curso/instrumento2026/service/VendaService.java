package com.curso.instrumento2026.service;

import com.curso.instrumento2026.domain.*;
import com.curso.instrumento2026.exception.RecursoNaoEncontradoException;
import com.curso.instrumento2026.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class VendaService {
    private final VendaRepository repository;
    private final ClienteRepository clienteRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final InstrumentoRepository instrumentoRepository;

    public VendaService(VendaRepository repository, ClienteRepository clienteRepository,
                        ColaboradorRepository colaboradorRepository, InstrumentoRepository instrumentoRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.instrumentoRepository = instrumentoRepository;
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
        Venda venda = buscarEntidade(id);
        if (venda.getStatus() != StatusVenda.ABERTA) {
            throw new IllegalStateException("Somente vendas abertas podem ser alteradas");
        }
        validarParticipantes(venda.getCliente(), venda.getVendedor());
        for (ItemVenda item : venda.getItens()) {
            if (item.getInstrumento().getStatus() != Status.ATIVO) {
                throw new IllegalArgumentException("Todos os instrumentos da venda devem estar ativos");
            }
            item.getInstrumento().retirarEstoque(item.getQuantidade().intValueExact());
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
