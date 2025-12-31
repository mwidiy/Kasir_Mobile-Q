const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

const getAllTables = async (req, res) => {
    try {
        const tables = await prisma.table.findMany({
            orderBy: { id: 'asc' }
        });
        res.json(tables);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const createTable = async (req, res) => {
    try {
        const { name, location, qrCode } = req.body;
        const newTable = await prisma.table.create({
            data: {
                name,
                location,
                qrCode,
                isActive: true
            }
        });
        res.json(newTable);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

const deleteTable = async (req, res) => {
    try {
        const { id } = req.params;
        const deletedTable = await prisma.table.delete({
            where: { id: parseInt(id) }
        });
        res.json(deletedTable);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

module.exports = { getAllTables, createTable, deleteTable };
