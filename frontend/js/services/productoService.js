import API from './api.js';

const ProductoService = {
    async listar() {
        return await API.get('productos');
    },
    
    async buscar(q) {
        return await API.get('productos/buscar', { q });
    },
    
    async buscarPorCodigo(codigo) {
        return await API.get('productos', { codigo });
    },
    
    async bajoStock() {
        return await API.get('productos/bajo-stock');
    },
    
    async guardar(producto) {
        // Validaciones en cliente antes de mandar
        if (!producto.codigo || producto.codigo.trim() === '') throw new Error('El código es obligatorio.');
        if (!producto.nombre || producto.nombre.trim() === '') throw new Error('El nombre es obligatorio.');
        if (parseFloat(producto.precioVenta) <= 0) throw new Error('El precio de venta debe ser mayor a cero.');
        if (parseInt(producto.stock) < 0) throw new Error('El stock no puede ser negativo.');
        
        if (producto.idProducto) {
            return await API.put('productos', producto);
        } else {
            return await API.post('productos', producto);
        }
    },
    
    async eliminar(id) {
        return await API.delete('productos', { id });
    },
    
    // Categorías y Proveedores asociados a la gestión de productos
    async listarCategorias() {
        return await API.get('categorias');
    },
    
    async listarProveedores() {
        return await API.get('proveedores');
    }
};

export default ProductoService;
